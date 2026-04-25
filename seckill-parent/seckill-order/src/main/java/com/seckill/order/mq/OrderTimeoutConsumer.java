package com.seckill.order.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.infrastructure.config.RabbitMQConfig;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.StockRollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时消费者
 * 消费死信队列中的超时消息，执行订单取消和库存回滚
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderMapper orderMapper;
    private final StockRollbackService stockRollbackService;
    private final RedisOperations redisOperations;

    private static final String LOCK_KEY_PREFIX = "order:timeout:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 30;

    /**
     * 消费订单超时消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_TIMEOUT_QUEUE)
    public void consume(OrderTimeoutMessage message, Message mqMessage, Channel channel) throws IOException {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        String orderNo = message.getOrderNo();

        log.info("收到订单超时消息, orderNo={}, userId={}, activityId={}, goodsId={}",
                orderNo, message.getUserId(), message.getActivityId(), message.getGoodsId());

        String lockKey = LOCK_KEY_PREFIX + orderNo;
        Boolean locked = redisOperations.setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        
        if (!Boolean.TRUE.equals(locked)) {
            log.info("订单超时处理中或已处理，跳过, orderNo={}", orderNo);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            processTimeout(message, orderNo, deliveryTag, channel);
        } finally {
            redisOperations.delete(lockKey);
        }
    }

    private void processTimeout(OrderTimeoutMessage message, String orderNo, long deliveryTag, Channel channel) throws IOException {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("订单不存在, orderNo={}", orderNo);
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (!OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            log.info("订单状态不是待支付，无需取消, orderNo={}, status={}", orderNo, order.getStatus());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            stockRollbackService.rollbackStock(
                    message.getActivityId(),
                    message.getGoodsId(),
                    message.getUserId(),
                    message.getQuantity() == null ? 1 : message.getQuantity()
            );
            log.info("订单超时库存回滚成功, orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("订单超时库存回滚失败, orderNo={}", orderNo, e);
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        int affected = orderMapper.cancelOrder(
                orderNo,
                message.getUserId(),
                OrderStatusEnum.PENDING_PAY.getCode(),
                OrderStatusEnum.CANCELLED.getCode(),
                "订单超时未支付，系统自动取消"
        );

        if (affected < 1) {
            log.warn("订单状态更新失败，可能已被处理, orderNo={}", orderNo);
            channel.basicAck(deliveryTag, false);
            return;
        }

        log.info("订单超时取消成功, orderNo={}", orderNo);
        channel.basicAck(deliveryTag, false);
    }
}
