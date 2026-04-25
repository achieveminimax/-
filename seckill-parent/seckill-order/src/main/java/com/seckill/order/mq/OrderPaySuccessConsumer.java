package com.seckill.order.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.infrastructure.config.RabbitMQConfig;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;

/**
 * 订单支付成功消费者
 * 处理支付成功后的业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaySuccessConsumer {

    private final OrderMapper orderMapper;

    /**
     * 消费支付成功消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PAY_SUCCESS_QUEUE)
    public void consume(PaySuccessMessage message, Message mqMessage, Channel channel) throws IOException {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        String orderNo = message.getOrderNo();
        String payNo = message.getPayNo();

        log.info("收到支付成功消息, orderNo={}, payNo={}", orderNo, payNo);

        try {
            // 1. 查询订单
            Order order = orderMapper.selectByOrderNo(orderNo);
            if (order == null) {
                log.warn("订单不存在, orderNo={}", orderNo);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 校验订单状态
            if (!OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
                log.warn("订单状态不是已支付, orderNo={}, status={}", orderNo, order.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. TODO: 更新秒杀记录状态为成功（需要 seckill-seckill 模块）
            // updateSeckillRecordStatus(order.getUserId(), order.getActivityId(), order.getGoodsId());

            // 4. TODO: 发送支付成功通知（邮件/短信）
            // 可以在这里调用邮件服务或短信服务

            log.info("支付成功消息处理完成, orderNo={}, payNo={}", orderNo, payNo);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理支付成功消息异常, orderNo={}, payNo={}", orderNo, payNo, e);
            // 异常时拒绝消息，不重新入队，避免无限重试
            channel.basicReject(deliveryTag, false);
        }
    }

    /**
     * 支付成功消息
     */
    @Data
    public static class PaySuccessMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        private String orderNo;
        private String payNo;
        private Long userId;
        private Long messageCreateTime;

        public PaySuccessMessage() {
            this.messageCreateTime = System.currentTimeMillis();
        }

        public PaySuccessMessage(String orderNo, String payNo) {
            this();
            this.orderNo = orderNo;
            this.payNo = payNo;
        }
    }
}
