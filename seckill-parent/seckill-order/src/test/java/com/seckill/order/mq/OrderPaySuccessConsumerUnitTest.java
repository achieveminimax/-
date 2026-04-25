package com.seckill.order.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPaySuccessConsumer 单元测试")
class OrderPaySuccessConsumerUnitTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private Channel channel;

    private OrderPaySuccessConsumer orderPaySuccessConsumer;

    private static final String TEST_ORDER_NO = "ORD2025042500001";
    private static final String TEST_PAY_NO = "PAY2025042500001";

    @BeforeEach
    void setUp() {
        orderPaySuccessConsumer = new OrderPaySuccessConsumer(orderMapper);
    }

    @Test
    @DisplayName("支付成功消息处理 - 订单不存在时ACK")
    void consume_OrderNotFound_ShouldAck() throws IOException {
        OrderPaySuccessConsumer.PaySuccessMessage message = new OrderPaySuccessConsumer.PaySuccessMessage(TEST_ORDER_NO, TEST_PAY_NO);
        Message mqMessage = createMqMessage();

        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(null);

        orderPaySuccessConsumer.consume(message, mqMessage, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("支付成功消息处理 - 订单状态非已支付时ACK")
    void consume_OrderNotPaid_ShouldAck() throws IOException {
        OrderPaySuccessConsumer.PaySuccessMessage message = new OrderPaySuccessConsumer.PaySuccessMessage(TEST_ORDER_NO, TEST_PAY_NO);
        Message mqMessage = createMqMessage();

        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);

        orderPaySuccessConsumer.consume(message, mqMessage, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("支付成功消息处理 - 订单已支付状态正常处理")
    void consume_OrderPaid_ShouldAck() throws IOException {
        OrderPaySuccessConsumer.PaySuccessMessage message = new OrderPaySuccessConsumer.PaySuccessMessage(TEST_ORDER_NO, TEST_PAY_NO);
        Message mqMessage = createMqMessage();

        Order order = createTestOrder(OrderStatusEnum.PAID.getCode());
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);

        orderPaySuccessConsumer.consume(message, mqMessage, channel);

        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("支付成功消息处理 - 异常时拒绝消息")
    void consume_Exception_ShouldReject() throws IOException {
        OrderPaySuccessConsumer.PaySuccessMessage message = new OrderPaySuccessConsumer.PaySuccessMessage(TEST_ORDER_NO, TEST_PAY_NO);
        Message mqMessage = createMqMessage();

        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenThrow(new RuntimeException("数据库异常"));

        orderPaySuccessConsumer.consume(message, mqMessage, channel);

        verify(channel).basicReject(1L, false);
    }

    @Test
    @DisplayName("PaySuccessMessage - 构造器测试")
    void testPaySuccessMessage() {
        OrderPaySuccessConsumer.PaySuccessMessage message = new OrderPaySuccessConsumer.PaySuccessMessage(TEST_ORDER_NO, TEST_PAY_NO);

        assertEquals(TEST_ORDER_NO, message.getOrderNo());
        assertEquals(TEST_PAY_NO, message.getPayNo());
        assertNotNull(message.getMessageCreateTime());
    }

    private Message createMqMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        return new Message(new byte[0], properties);
    }

    private Order createTestOrder(Integer status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(TEST_ORDER_NO);
        order.setUserId(1L);
        order.setActivityId(1L);
        order.setGoodsId(1L);
        order.setGoodsName("iPhone 15");
        order.setOrderPrice(new BigDecimal("5999.00"));
        order.setQuantity(1);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }
}
