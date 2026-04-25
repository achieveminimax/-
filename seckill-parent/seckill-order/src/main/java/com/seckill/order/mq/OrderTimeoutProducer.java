package com.seckill.order.mq;

import com.seckill.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息生产者
 * 订单创建成功后发送延迟消息，TTL到期后触发取消逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时延迟消息
     *
     * @param message 订单超时消息
     */
    public void send(OrderTimeoutMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_TIMEOUT_ROUTING_KEY,
                    message
            );
            log.info("订单超时延迟消息发送成功, orderNo={}, userId={}, activityId={}, goodsId={}",
                    message.getOrderNo(), message.getUserId(), message.getActivityId(), message.getGoodsId());
        } catch (Exception e) {
            log.error("订单超时延迟消息发送失败, orderNo={}, userId={}",
                    message.getOrderNo(), message.getUserId(), e);
            throw e;
        }
    }
}
