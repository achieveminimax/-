package com.seckill.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.common.utils.UserContext;
import com.seckill.order.dto.OrderDetailResponse;
import com.seckill.order.dto.OrderListResponse;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private StockRollbackService stockRollbackService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private MockedStatic<UserContext> userContextMock;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_ORDER_NO = "ORD2025042500001";

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getCurrentUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    @Test
    void listUserOrders_Success() {
        // 准备测试数据
        Order order1 = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());
        Order order2 = createTestOrder(2L, "ORD2025042500002", OrderStatusEnum.PAID.getCode());

        Page<Order> pageResult = new Page<>(1, 10);
        pageResult.setRecords(Arrays.asList(order1, order2));
        pageResult.setTotal(2);

        when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(pageResult);

        // 执行测试
        PageResult<OrderListResponse> result = orderService.listUserOrders(null, 1L, 10L);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(TEST_ORDER_NO, result.getRecords().get(0).getOrderNo());

        verify(orderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listUserOrders_WithStatusFilter() {
        // 准备测试数据
        Order order = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        Page<Order> pageResult = new Page<>(1, 10);
        pageResult.setRecords(Collections.singletonList(order));
        pageResult.setTotal(1);

        when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(pageResult);

        // 执行测试
        PageResult<OrderListResponse> result = orderService.listUserOrders(
                OrderStatusEnum.PENDING_PAY.getCode(), 1L, 10L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(OrderStatusEnum.PENDING_PAY.getCode(), result.getRecords().get(0).getStatus());

        verify(orderMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void listUserOrders_Unauthorized() {
        // 模拟未登录
        userContextMock.when(UserContext::getCurrentUserId).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.listUserOrders(null, 1L, 10L));

        assertEquals(ResponseCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
    }

    @Test
    void getOrderDetail_Success() {
        // 准备测试数据
        Order order = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);

        // 执行测试
        OrderDetailResponse result = orderService.getOrderDetail(TEST_ORDER_NO);

        // 验证结果
        assertNotNull(result);
        assertEquals(TEST_ORDER_NO, result.getOrderNo());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("iPhone 15", result.getGoodsName());

        verify(orderMapper).selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID);
    }

    @Test
    void getOrderDetail_OrderNotFound() {
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(TEST_ORDER_NO));

        assertEquals(ResponseCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void getOrderDetail_EmptyOrderNo() {
        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(""));

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void cancelOrder_Success() {
        // 准备测试数据
        Order order = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        when(orderMapper.cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString()))
                .thenReturn(1);

        // 执行测试
        assertDoesNotThrow(() -> orderService.cancelOrder(TEST_ORDER_NO, "不想要了"));

        // 验证调用
        verify(stockRollbackService).rollbackStock(
                order.getActivityId(), order.getGoodsId(), TEST_USER_ID, order.getQuantity());
        verify(orderMapper).cancelOrder(eq(TEST_ORDER_NO), eq(TEST_USER_ID), anyInt(), anyInt(), anyString());
    }

    @Test
    void cancelOrder_OrderNotFound() {
        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(TEST_ORDER_NO, null));

        assertEquals(ResponseCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void cancelOrder_StatusNotAllow() {
        // 准备测试数据 - 已支付的订单
        Order order = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PAID.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(TEST_ORDER_NO, null));

        assertEquals(ResponseCodeEnum.ORDER_STATUS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void cancelOrder_StockRollbackFailed() {
        // 准备测试数据
        Order order = createTestOrder(1L, TEST_ORDER_NO, OrderStatusEnum.PENDING_PAY.getCode());

        when(orderMapper.selectOwnedOrder(TEST_ORDER_NO, TEST_USER_ID)).thenReturn(order);
        doThrow(new RuntimeException("库存回滚失败"))
                .when(stockRollbackService)
                .rollbackStock(anyLong(), anyLong(), anyLong(), anyInt());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(TEST_ORDER_NO, null));

        assertEquals(ResponseCodeEnum.ERROR.getCode(), exception.getCode());
    }

    /**
     * 创建测试订单
     */
    private Order createTestOrder(Long id, String orderNo, Integer status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(TEST_USER_ID);
        order.setActivityId(1L);
        order.setGoodsId(1L);
        order.setGoodsName("iPhone 15");
        order.setGoodsImage("https://example.com/image.jpg");
        order.setOrderPrice(new BigDecimal("5999.00"));
        order.setQuantity(1);
        order.setStatus(status);
        order.setReceiverName("张三");
        order.setReceiverPhone("13800138000");
        order.setReceiverAddress("北京市|北京市|朝阳区|xxx街道");
        order.setCreateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }
}
