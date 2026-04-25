package com.seckill.order.service;

import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.impl.OrderAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAdminServiceImpl 单元测试")
class OrderAdminServiceImplUnitTest {

    @Mock
    private OrderMapper orderMapper;

    private OrderAdminServiceImpl orderAdminService;

    private static final String TEST_ORDER_NO = "ORD2025042500001";

    @BeforeEach
    void setUp() {
        orderAdminService = new OrderAdminServiceImpl(orderMapper);
    }

    @Test
    @DisplayName("发货 - 快递公司为空抛出异常")
    void shipOrder_BlankExpressCompany_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderAdminService.shipOrder(TEST_ORDER_NO, "", "SF123456"));
        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发货 - 快递单号为空抛出异常")
    void shipOrder_BlankExpressNo_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderAdminService.shipOrder(TEST_ORDER_NO, "顺丰", "  "));
        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发货 - 订单不存在抛出异常")
    void shipOrder_OrderNotFound_ThrowsBusinessException() {
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderAdminService.shipOrder(TEST_ORDER_NO, "顺丰", "SF123456"));
        assertEquals(ResponseCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发货 - 订单状态不允许发货抛出异常")
    void shipOrder_OrderStatusNotAllowShip_ThrowsBusinessException() {
        Order order = createTestOrder(OrderStatusEnum.PENDING_PAY.getCode());
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderAdminService.shipOrder(TEST_ORDER_NO, "顺丰", "SF123456"));
        assertEquals(ResponseCodeEnum.ORDER_STATUS_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("发货 - 成功更新快递信息和订单状态")
    void shipOrder_Success_UpdatesOrder() {
        Order order = createTestOrder(OrderStatusEnum.PAID.getCode());
        when(orderMapper.selectByOrderNo(TEST_ORDER_NO)).thenReturn(order);

        orderAdminService.shipOrder(TEST_ORDER_NO, " 顺丰 ", " SF123456 ");

        assertEquals("顺丰", order.getExpressCompany());
        assertEquals("SF123456", order.getExpressNo());
        assertNotNull(order.getShipTime());
        assertEquals(OrderStatusEnum.SHIPPED.getCode(), order.getStatus());
        verify(orderMapper).updateById(order);
    }

    private Order createTestOrder(Integer status) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo(TEST_ORDER_NO);
        order.setUserId(1L);
        order.setActivityId(1L);
        order.setGoodsId(1L);
        order.setGoodsName("测试商品");
        order.setOrderPrice(new BigDecimal("99.00"));
        order.setQuantity(1);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }
}
