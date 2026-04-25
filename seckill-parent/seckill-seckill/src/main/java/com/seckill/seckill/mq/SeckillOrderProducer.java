package com.seckill.seckill.mq;

import com.seckill.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeckillOrderProducer {

    public static final String RETRY_COUNT_HEADER = "x-seckill-retry-count";

    private final RabbitTemplate rabbitTemplate;

    public void send(SeckillOrderMessage message) {
        send(message, 0);
    }

    public void send(SeckillOrderMessage message, int retryCount) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                message,
                rawMessage -> setRetryCount(rawMessage, retryCount)
        );
    }

    private Message setRetryCount(Message message, int retryCount) {
        message.getMessageProperties().setHeader(RETRY_COUNT_HEADER, retryCount);
        return message;
    }
}
