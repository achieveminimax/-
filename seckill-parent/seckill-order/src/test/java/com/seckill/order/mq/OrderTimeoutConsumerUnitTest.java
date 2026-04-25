package com.seckill.order.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.StockRollbackService;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTimeoutConsumer 单元测试")
class OrderTimeoutConsumerUnitTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private StockRollbackService stockRollbackService;

    @Mock
    private RedisOperations redisOperations;

    private OrderTimeoutConsumer orderTimeoutConsumer;

    private static final String TEST_ORDER_NO = "ORD2025042500001";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_ACTIVITY_ID = 1L;
    private static final Long TEST_GOODS_ID = 1L;

    @BeforeEach
    void setUp() {
        orderTimeoutConsumer = new OrderTimeoutConsumer(orderMapper, stockRollbackService, redisOperations);
    }

    @Test
    @DisplayName("分布式锁获取成功 - 订单不存在时直接ACK")
    void consume_OrderNotFound_ShouldAck() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(null);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("分布式锁获取失败时跳过处理")
    void consume_LockFailed_ShouldSkip() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(channel).basicAck(1L, false);
        verify(orderMapper, never()).selectByOrderNo(anyString());
    }

    @Test
    @DisplayName("订单状态不是待支付 - 无需取消ACK")
    void consume_OrderAlreadyPaid_ShouldAck() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);
        Order order = createTestOrder(OrderStatusEnum.PAID.getCode());

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(channel).basicAck(1L, false);
        verify(stockRollbackService, never()).rollbackStock(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("订单超时完整处理 - 库存回滚+订单取消成功ACK")
    void consume_TimeoutOrder_Success() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);
        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);
        when(orderMapper.cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString()))
                .thenReturn(1);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(stockRollbackService).rollbackStock(TEST_ACTIVITY_ID, TEST_GOODS_ID, TEST_USER_ID, 1);
        verify(orderMapper).cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString());
        verify(channel).basicAck(1L, false);
        verify(redisOperations).delete(contains("order:timeout:lock:"));
    }

    @Test
    @DisplayName("库存回滚失败 - NACK重入队列")
    void consume_StockRollbackFail_ShouldNack() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);
        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);
        doThrow(new RuntimeException("Redis error")).when(stockRollbackService)
                .rollbackStock(anyLong(), anyLong(), anyLong(), anyInt());

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(channel).basicNack(1L, false, true);
        verify(orderMapper, never()).cancelOrder(anyString(), anyLong(), anyInt(), anyInt(), anyString());
        verify(redisOperations).delete(contains("order:timeout:lock:"));
    }

    @Test
    @DisplayName("订单取消影响行数为0 - ACK（已被其他处理）")
    void consume_CancelAffectsZero_ShouldAck() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);
        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);
        when(orderMapper.cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString()))
                .thenReturn(0);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(channel).basicAck(1L, false);
        verify(redisOperations).delete(contains("order:timeout:lock:"));
    }

    @Test
    @DisplayName("quantity为null时默认使用1")
    void consume_QuantityNull_DefaultToOne() throws IOException {
        OrderTimeoutMessage message = timeoutMessage();
        message.setQuantity(null);
        Message mqMessage = mqMessage();
        Channel channel = mock(Channel.class);
        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());

        when(redisOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);
        when(orderMapper.cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString()))
                .thenReturn(1);

        orderTimeoutConsumer.consume(message, mqMessage, channel);
        verify(stockRollbackService).rollbackStock(TEST_ACTIVITY_ID, TEST_GOODS_ID, TEST_USER_ID, 1);
    }

    @Test
    @DisplayName("超时消息创建 - 属性正确")
    void timeoutMessage_PropertiesCorrect() {
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderNo(TEST_ORDER_NO);
        message.setUserId(TEST_USER_ID);
        message.setActivityId(TEST_ACTIVITY_ID);
        message.setGoodsId(TEST_GOODS_ID);
        message.setQuantity(1);
        message.setSeckillRecordId(100L);

        assertEquals(TEST_ORDER_NO, message.getOrderNo());
        assertEquals(TEST_USER_ID, message.getUserId());
        assertEquals(TEST_ACTIVITY_ID, message.getActivityId());
        assertEquals(TEST_GOODS_ID, message.getGoodsId());
        assertEquals(1, message.getQuantity());
        assertEquals(100L, message.getSeckillRecordId());
        assertNotNull(message.getMessageCreateTime());
    }

    private OrderTimeoutMessage timeoutMessage() {
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderNo(TEST_ORDER_NO);
        message.setUserId(TEST_USER_ID);
        message.setActivityId(TEST_ACTIVITY_ID);
        message.setGoodsId(TEST_GOODS_ID);
        message.setQuantity(1);
        return message;
    }

    private Message mqMessage() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        return new Message(new byte[0], props);
    }

    private Order createTestOrder(Integer status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(TEST_ORDER_NO);
        order.setUserId(TEST_USER_ID);
        order.setActivityId(TEST_ACTIVITY_ID);
        order.setGoodsId(TEST_GOODS_ID);
        order.setGoodsName("iPhone 15");
        order.setOrderPrice(new BigDecimal("5999.00"));
        order.setQuantity(1);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }
}
