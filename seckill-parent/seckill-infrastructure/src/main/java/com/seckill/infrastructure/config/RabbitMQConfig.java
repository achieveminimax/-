package com.seckill.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 定义 Exchange、Queue、Binding 以及消息转换器
 *
 * @author seckill
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ==================== Exchange 定义 ====================
    public static final String SECKILL_EXCHANGE = "seckill.exchange";

    // ==================== Queue 定义 ====================
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String ORDER_PAY_SUCCESS_QUEUE = "order.pay.success.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_DLX_QUEUE = "order.timeout.dlx.queue";
    public static final String MAIL_SEND_QUEUE = "mail.send.queue";

    // ==================== Routing Key 定义 ====================
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";
    public static final String ORDER_PAY_SUCCESS_ROUTING_KEY = "order.pay.success";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String ORDER_TIMEOUT_DLX_ROUTING_KEY = "order.timeout.dlx";
    public static final String MAIL_SEND_ROUTING_KEY = "mail.send";

    // ==================== 延迟队列配置 ====================
    // 订单超时时间：15 分钟（单位：毫秒）
    public static final long ORDER_TIMEOUT_TTL = 15 * 60 * 1000;

    /**
     * 消息转换器（使用 Jackson JSON）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // 消息确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: {}", correlationData);
            } else {
                log.error("消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });

        // 消息返回回调（当消息无法路由到队列时触发）
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
        });

        return rabbitTemplate;
    }

    // ==================== Exchange 声明 ====================

    /**
     * 秒杀 Exchange（Topic 类型）
     */
    @Bean
    public TopicExchange seckillExchange() {
        return ExchangeBuilder.topicExchange(SECKILL_EXCHANGE)
                .durable(true)
                .build();
    }

    // ==================== Queue 声明 ====================

    /**
     * 秒杀订单队列
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                // 设置队列最大长度
                .withArgument("x-max-length", 100000)
                // 设置消息 TTL（60秒）
                .withArgument("x-message-ttl", 60000)
                .build();
    }

    /**
     * 支付成功队列
     */
    @Bean
    public Queue orderPaySuccessQueue() {
        return QueueBuilder.durable(ORDER_PAY_SUCCESS_QUEUE)
                .build();
    }

    /**
     * 订单超时队列（延迟队列）
     * 消息过期后转发到死信队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                // 设置消息 TTL（15分钟）
                .withArgument("x-message-ttl", ORDER_TIMEOUT_TTL)
                // 设置死信交换机
                .withArgument("x-dead-letter-exchange", SECKILL_EXCHANGE)
                // 设置死信路由键
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_DLX_ROUTING_KEY)
                .build();
    }

    /**
     * 订单超时死信队列
     * 接收超时未支付的订单消息
     */
    @Bean
    public Queue orderTimeoutDlxQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DLX_QUEUE)
                .build();
    }

    /**
     * 邮件发送队列
     */
    @Bean
    public Queue mailSendQueue() {
        return QueueBuilder.durable(MAIL_SEND_QUEUE)
                .build();
    }

    // ==================== Binding 声明 ====================

    /**
     * 秒杀订单队列绑定
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder
                .bind(seckillOrderQueue())
                .to(seckillExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    /**
     * 支付成功队列绑定
     */
    @Bean
    public Binding orderPaySuccessBinding() {
        return BindingBuilder
                .bind(orderPaySuccessQueue())
                .to(seckillExchange())
                .with(ORDER_PAY_SUCCESS_ROUTING_KEY);
    }

    /**
     * 订单超时队列绑定
     */
    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder
                .bind(orderTimeoutQueue())
                .to(seckillExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    /**
     * 订单超时死信队列绑定
     */
    @Bean
    public Binding orderTimeoutDlxBinding() {
        return BindingBuilder
                .bind(orderTimeoutDlxQueue())
                .to(seckillExchange())
                .with(ORDER_TIMEOUT_DLX_ROUTING_KEY);
    }

    /**
     * 邮件发送队列绑定
     */
    @Bean
    public Binding mailSendBinding() {
        return BindingBuilder
                .bind(mailSendQueue())
                .to(seckillExchange())
                .with(MAIL_SEND_ROUTING_KEY);
    }

}
