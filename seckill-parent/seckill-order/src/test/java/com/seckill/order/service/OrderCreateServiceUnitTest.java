package com.seckill.order.service;

import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.order.dto.CreateSeckillOrderCommand;
import com.seckill.order.dto.CreateSeckillOrderResult;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.mq.OrderTimeoutProducer;
import com.seckill.order.service.impl.OrderCreateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreateService 单元测试")
class OrderCreateServiceUnitTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private OrderTimeoutProducer orderTimeoutProducer;

    @Test
    @DisplayName("创建订单幂等 - orderNo 已存在时直接返回已有订单")
    void createSeckillOrder_Idempotent_WhenOrderExists() {
        OrderCreateServiceImpl service = buildService();
        Order existed = new Order();
        existed.setId(9001L);
        existed.setOrderNo("ORD-1");

        when(orderMapper.selectByOrderNo("ORD-1")).thenReturn(existed);

        CreateSeckillOrderResult result = service.createSeckillOrder(command());

        assertEquals(9001L, result.getOrderId());
        assertEquals("ORD-1", result.getOrderNo());
        verify(goodsMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("创建订单失败 - 商品库存扣减失败")
    void createSeckillOrder_Fail_WhenGoodsStockNotEnough() {
        OrderCreateServiceImpl service = buildService();

        when(orderMapper.selectByOrderNo("ORD-1")).thenReturn(null);
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity());
        when(goodsMapper.selectById(2001L)).thenReturn(goods());
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());
        when(goodsMapper.deductStock(2001L, 1)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createSeckillOrder(command()));

        assertEquals(ResponseCodeEnum.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建订单成功 - 写入待支付订单快照")
    void createSeckillOrder_Success_ShouldInsertPendingOrder() {
        OrderCreateServiceImpl service = buildService();

        when(orderMapper.selectByOrderNo("ORD-1")).thenReturn(null);
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity());
        when(goodsMapper.selectById(2001L)).thenReturn(goods());
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());
        when(goodsMapper.deductStock(2001L, 1)).thenReturn(1);
        when(seckillGoodsMapper.incrementSalesCount(1L, 2001L, 1)).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(7001L);
            return 1;
        });

        CreateSeckillOrderResult result = service.createSeckillOrder(command());
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        verify(orderMapper).insert(captor.capture());
        Order order = captor.getValue();

        assertEquals(7001L, result.getOrderId());
        assertEquals("ORD-1", result.getOrderNo());
        assertEquals(OrderStatusEnum.PENDING_PAY.getCode(), order.getStatus());
        assertEquals("测试商品", order.getGoodsName());
        assertEquals("张三", order.getReceiverName());
        assertEquals("深圳市南山区科技园", order.getReceiverAddress());
        assertTrue(order.getDeleted() == 0);
    }

    private OrderCreateServiceImpl buildService() {
        return new OrderCreateServiceImpl(orderMapper, goodsMapper, seckillGoodsMapper, seckillActivityMapper, orderTimeoutProducer);
    }

    private CreateSeckillOrderCommand command() {
        CreateSeckillOrderCommand command = new CreateSeckillOrderCommand();
        command.setRecordId(9001L);
        command.setOrderNo("ORD-1");
        command.setUserId(1001L);
        command.setActivityId(1L);
        command.setGoodsId(2001L);
        command.setQuantity(1);
        command.setAddressId(3001L);
        command.setReceiverName("张三");
        command.setReceiverPhone("13800138000");
        command.setReceiverAddress("深圳市南山区科技园");
        return command;
    }

    private Goods goods() {
        Goods goods = new Goods();
        goods.setId(2001L);
        goods.setName("测试商品");
        goods.setCoverImage("https://example.com/cover.jpg");
        goods.setDeleted(0);
        return goods;
    }

    private SeckillActivity activity() {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setActivityName("测试活动");
        return activity;
    }

    private SeckillGoods seckillGoods() {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(1L);
        seckillGoods.setGoodsId(2001L);
        seckillGoods.setSeckillPrice(new BigDecimal("99.00"));
        seckillGoods.setLimitPerUser(1);
        return seckillGoods;
    }
}
