package com.seckill.seckill.service;

import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.seckill.config.SeckillProperties;
import com.seckill.seckill.dto.SeckillResultResponse;
import com.seckill.seckill.entity.SeckillRecord;
import com.seckill.seckill.mapper.SeckillRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillRecordService 单元测试")
class SeckillRecordServiceUnitTest {

    @Mock
    private SeckillRecordMapper seckillRecordMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RedisUtils redisUtils;

    private SeckillRecordService seckillRecordService;

    @BeforeEach
    void setUp() {
        SeckillProperties properties = new SeckillProperties();
        properties.setResultTtlSeconds(86400);
        seckillRecordService = new SeckillRecordService(seckillRecordMapper, orderMapper, redisUtils, properties);
    }

    @Test
    @DisplayName("创建排队中记录成功")
    void createQueuedRecord_Success() {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);
        record.setStatus(SeckillRecordStatusEnum.QUEUING.getCode());

        when(seckillRecordMapper.insert(any(SeckillRecord.class))).thenAnswer(invocation -> {
            SeckillRecord r = invocation.getArgument(0);
            r.setId(9001L);
            return 1;
        });

        SeckillRecord result = seckillRecordService.createQueuedRecord(1001L, 1L, 2001L);

        assertNotNull(result);
        assertEquals(9001L, result.getId());
        assertEquals(SeckillRecordStatusEnum.QUEUING.getCode(), result.getStatus());
    }

    @Test
    @DisplayName("标记秒杀成功 - 更新记录并缓存结果")
    void markSuccess_Success() {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);
        record.setStatus(SeckillRecordStatusEnum.QUEUING.getCode());

        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);

        seckillRecordService.markSuccess(9001L, 7001L, "ORD-20240101-001", goods, seckillGoods);

        verify(seckillRecordMapper).updateById(any(SeckillRecord.class));
        verify(redisUtils).set(anyString(), any(SeckillResultResponse.class), anyLong(), any());
    }

    @Test
    @DisplayName("标记秒杀失败 - 更新记录并缓存结果")
    void markFailed_Success() {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);
        record.setStatus(SeckillRecordStatusEnum.QUEUING.getCode());

        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);

        seckillRecordService.markFailed(9001L, "库存不足", goods, seckillGoods);

        verify(seckillRecordMapper).updateById(any(SeckillRecord.class));
        verify(redisUtils).set(anyString(), any(SeckillResultResponse.class), anyLong(), any());
    }

    @Test
    @DisplayName("查询秒杀结果 - 从缓存获取")
    void getResult_FromCache() {
        SeckillResultResponse cached = new SeckillResultResponse();
        cached.setRecordId(9001L);
        cached.setStatus(SeckillRecordStatusEnum.SUCCESS.getCode());
        cached.setOrderNo("ORD-20240101-001");

        when(redisUtils.get(anyString())).thenReturn(cached);

        SeckillResultResponse result = seckillRecordService.getResult(1001L, 9001L, goods(), seckillGoods());

        assertNotNull(result);
        assertEquals(9001L, result.getRecordId());
        assertEquals("ORD-20240101-001", result.getOrderNo());
    }

    @Test
    @DisplayName("查询秒杀结果 - 从数据库获取")
    void getResult_FromDatabase() {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);
        record.setStatus(SeckillRecordStatusEnum.SUCCESS.getCode());
        record.setOrderId(7001L);

        Order order = new Order();
        order.setId(7001L);
        order.setOrderNo("ORD-20240101-001");

        when(redisUtils.get(anyString())).thenReturn(null);
        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(orderMapper.selectById(7001L)).thenReturn(order);

        SeckillResultResponse result = seckillRecordService.getResult(1001L, 9001L, goods(), seckillGoods());

        assertNotNull(result);
        assertEquals(9001L, result.getRecordId());
        assertEquals("ORD-20240101-001", result.getOrderNo());
        verify(redisUtils).set(anyString(), any(SeckillResultResponse.class), anyLong(), any());
    }

    @Test
    @DisplayName("查询秒杀结果失败 - 记录不存在")
    void getResult_Fail_WhenRecordNotFound() {
        when(redisUtils.get(anyString())).thenReturn(null);
        when(seckillRecordMapper.selectById(9001L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillRecordService.getResult(1001L, 9001L, goods(), seckillGoods()));

        assertEquals(ResponseCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("查询秒杀结果失败 - 无权访问他人记录")
    void getResult_Fail_WhenAccessOthersRecord() {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1002L); // 其他用户
        record.setActivityId(1L);
        record.setGoodsId(2001L);

        when(redisUtils.get(anyString())).thenReturn(null);
        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillRecordService.getResult(1001L, 9001L, goods(), seckillGoods()));

        assertEquals(ResponseCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    private Goods goods() {
        Goods goods = new Goods();
        goods.setId(2001L);
        goods.setName("测试商品");
        return goods;
    }

    private SeckillGoods seckillGoods() {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(1L);
        seckillGoods.setGoodsId(2001L);
        seckillGoods.setSeckillPrice(new BigDecimal("99.00"));
        return seckillGoods;
    }
}
