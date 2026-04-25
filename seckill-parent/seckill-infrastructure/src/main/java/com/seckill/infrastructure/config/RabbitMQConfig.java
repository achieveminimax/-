package com.seckill.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ==================== 队列名称 ====================
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_DLX_QUEUE = "seckill.order.dlx.queue";
    public static final String ORDER_PAY_SUCCESS_QUEUE = "order.pay.success.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String MAIL_SEND_QUEUE = "mail.send.queue";

    // ==================== 交换机名称 ====================
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String DELAY_EXCHANGE = "delay.exchange";

    // ==================== 路由键 ====================
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";
    public static final String SECKILL_ORDER_DLX_ROUTING_KEY = "seckill.order.dlx";
    public static final String ORDER_PAY_SUCCESS_ROUTING_KEY = "order.pay.success";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
    public static final String MAIL_SEND_ROUTING_KEY = "mail.send";

    // ==================== 延迟队列配置 ====================
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "order.timeout.delay.queue";
    public static final String ORDER_TIMEOUT_TTL = "900000"; // 15分钟（毫秒）

    /**
     * 消息转换器（使用 Jackson）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // 开启消息确认机制
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: {}", correlationData);
            } else {
                log.error("消息发送失败: {}, 原因: {}", correlationData, cause);
            }
        });

        // 开启消息返回机制（路由失败时回调）
        template.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, message={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getMessage());
        });

        // 强制消息返回
        template.setMandatory(true);

        log.info("RabbitTemplate 配置完成");
        return template;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);

        // 并发消费者配置
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(20);

        // 预取数量
        factory.setPrefetchCount(10);

        // 开启手动 ACK
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    // ==================== 交换机配置 ====================

    /**
     * 秒杀交换机（直连）
     */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    /**
     * 订单交换机（直连）
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 延迟交换机（直连）
     */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    // ==================== 队列配置 ====================

    /**
     * 秒杀订单队列
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SECKILL_ORDER_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue seckillOrderDlxQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_DLX_QUEUE).build();
    }

    /**
     * 支付成功队列
     */
    @Bean
    public Queue orderPaySuccessQueue() {
        return QueueBuilder.durable(ORDER_PAY_SUCCESS_QUEUE).build();
    }

    /**
     * 订单超时延迟队列（带TTL）
     */
    @Bean
    public Queue orderTimeoutDelayQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", DELAY_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTING_KEY)
                .withArgument("x-message-ttl", Integer.parseInt(ORDER_TIMEOUT_TTL))
                .build();
    }

    /**
     * 订单超时处理队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    /**
     * 邮件发送队列
     */
    @Bean
    public Queue mailSendQueue() {
        return QueueBuilder.durable(MAIL_SEND_QUEUE).build();
    }

    // ==================== 绑定配置 ====================

    /**
     * 秒杀订单队列绑定
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillExchange())
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public Binding seckillOrderDlxBinding() {
        return BindingBuilder.bind(seckillOrderDlxQueue())
                .to(seckillDlxExchange())
                .with(SECKILL_ORDER_DLX_ROUTING_KEY);
    }

    /**
     * 支付成功队列绑定
     */
    @Bean
    public Binding orderPaySuccessBinding() {
        return BindingBuilder.bind(orderPaySuccessQueue())
                .to(orderExchange())
                .with(ORDER_PAY_SUCCESS_ROUTING_KEY);
    }

    /**
     * 订单超时队列绑定
     */
    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(delayExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    /**
     * 邮件发送队列绑定
     */
    @Bean
    public Binding mailSendBinding() {
        return BindingBuilder.bind(mailSendQueue())
                .to(orderExchange())
                .with(MAIL_SEND_ROUTING_KEY);
    }
}
