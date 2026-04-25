package com.seckill.order.service;

import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.PayStatusEnum;
import com.seckill.common.enums.PayTypeEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.SnowflakeIdWorker;
import com.seckill.common.utils.UserContext;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.dto.PayCreateResponse;
import com.seckill.order.dto.PayStatusResponse;
import com.seckill.order.entity.Order;
import com.seckill.order.entity.PayRecord;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.mapper.PayRecordMapper;
import com.seckill.order.service.impl.PayServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayServiceUnitTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PayRecordMapper payRecordMapper;

    @Mock
    private RedisOperations redisOperations;

    @Mock
    private RabbitOperations rabbitOperations;

    @Mock
    private SnowflakeIdWorker snowflakeIdWorker;

    private PayServiceImpl payService;

    private MockedStatic<UserContext> userContextMock;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_ORDER_NO = "ORD2025042500001";
    private static final String TEST_PAY_NO = "PAY2025042500001";

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getCurrentUserId).thenReturn(TEST_USER_ID);
        payService = new PayServiceImpl(orderMapper, payRecordMapper, redisOperations, rabbitOperations, snowflakeIdWorker);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Test
    void createPay_Success() {
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        when(payRecordMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(null);
        when(snowflakeIdWorker.nextId()).thenReturn(123456789L);

        PayCreateResponse result = payService.createPay(TEST_ORDER_NO, PayTypeEnum.ALIPAY.getCode());

        assertNotNull(result);
        assertNotNull(result.getPayNo());
        assertEquals(TEST_ORDER_NO, result.getOrderNo());
        assertEquals(new BigDecimal("5999.00"), result.getPayAmount());
        assertEquals(PayTypeEnum.ALIPAY.getCode(), result.getPayType());

        verify(payRecordMapper).insert(any(PayRecord.class));
    }

    @Test
    void createPay_OrderNotFound() {
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> payService.createPay(TEST_ORDER_NO, PayTypeEnum.ALIPAY.getCode()));

        assertEquals(ResponseCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void createPay_OrderAlreadyPaid() {
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PAID.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> payService.createPay(TEST_ORDER_NO, PayTypeEnum.ALIPAY.getCode()));

        assertEquals(ResponseCodeEnum.ORDER_ALREADY_PAID.getCode(), exception.getCode());
    }

    @Test
    void createPay_InvalidPayType() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> payService.createPay(TEST_ORDER_NO, 999));

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void createPay_ExistingPendingRecord() {
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());
        PayRecord existingRecord = createTestPayRecord(TEST_PAY_NO, PayStatusEnum.PENDING.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        when(payRecordMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(existingRecord);

        PayCreateResponse result = payService.createPay(TEST_ORDER_NO, PayTypeEnum.ALIPAY.getCode());

        assertNotNull(result);
        assertEquals(TEST_PAY_NO, result.getPayNo());

        verify(payRecordMapper, never()).insert(any(PayRecord.class));
    }

    @Test
    void payCallback_Success() {
        PayRecord payRecord = createTestPayRecord(TEST_PAY_NO, PayStatusEnum.PENDING.getCode());
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(payRecordMapper.selectByPayNo(TEST_PAY_NO)).thenReturn(payRecord);
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);
        when(payRecordMapper.updateStatus(eq(TEST_PAY_NO), anyInt(), anyString(), any()))
                .thenReturn(1);

        assertDoesNotThrow(() -> payService.payCallback(TEST_PAY_NO, "MOCK123456"));

        verify(payRecordMapper).updateStatus(eq(TEST_PAY_NO), eq(PayStatusEnum.PAID.getCode()), anyString(), any());
        verify(orderMapper).updateById(any(Order.class));
    }

    @Test
    void payCallback_PayRecordNotFound() {
        when(payRecordMapper.selectByPayNo(TEST_PAY_NO)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> payService.payCallback(TEST_PAY_NO, "MOCK123456"));

        assertEquals(ResponseCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void payCallback_AlreadyPaid() {
        PayRecord payRecord = createTestPayRecord(TEST_PAY_NO, PayStatusEnum.PAID.getCode());

        when(payRecordMapper.selectByPayNo(TEST_PAY_NO)).thenReturn(payRecord);

        assertDoesNotThrow(() -> payService.payCallback(TEST_PAY_NO, "MOCK123456"));

        verify(payRecordMapper, never()).updateStatus(anyString(), anyInt(), anyString(), any());
    }

    @Test
    void queryPayStatus_Success() {
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PAID.getCode());
        PayRecord payRecord = createTestPayRecord(TEST_PAY_NO, PayStatusEnum.PAID.getCode());
        payRecord.setPayTime(LocalDateTime.now());

        when(redisOperations.get(anyString())).thenReturn(null);
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        when(payRecordMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(payRecord);

        PayStatusResponse result = payService.queryPayStatus(TEST_ORDER_NO);

        assertNotNull(result);
        assertEquals(TEST_ORDER_NO, result.getOrderNo());
        assertEquals(TEST_PAY_NO, result.getPayNo());
        assertEquals(PayStatusEnum.PAID.getCode(), result.getStatus());
    }

    @Test
    void queryPayStatus_OrderNotFound() {
        when(redisOperations.get(anyString())).thenReturn(null);
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> payService.queryPayStatus(TEST_ORDER_NO));

        assertEquals(ResponseCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void queryPayStatus_NoPayRecord() {
        Order order = createTestOrder(TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(redisOperations.get(anyString())).thenReturn(null);
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        when(payRecordMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(null);

        PayStatusResponse result = payService.queryPayStatus(TEST_ORDER_NO);

        assertNotNull(result);
        assertEquals(TEST_ORDER_NO, result.getOrderNo());
        assertEquals(PayStatusEnum.PENDING.getCode(), result.getStatus());
    }

    private Order createTestOrder(String orderNo, Integer status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setUserId(TEST_USER_ID);
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

    private PayRecord createTestPayRecord(String payNo, Integer status) {
        PayRecord payRecord = new PayRecord();
        payRecord.setId(1L);
        payRecord.setPayNo(payNo);
        payRecord.setOrderNo(TEST_ORDER_NO);
        payRecord.setPayMethod(PayTypeEnum.ALIPAY.getCode());
        payRecord.setPayAmount(new BigDecimal("5999.00"));
        payRecord.setStatus(status);
        payRecord.setCreateTime(LocalDateTime.now());
        return payRecord;
    }
}
