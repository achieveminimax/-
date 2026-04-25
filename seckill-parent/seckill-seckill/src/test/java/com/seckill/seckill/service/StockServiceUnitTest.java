package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.seckill.config.SeckillProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 单元测试")
class StockServiceUnitTest {

    @Mock
    private RedisOperations redisOperations;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        SeckillProperties properties = new SeckillProperties();
        properties.setDoneTtlSeconds(86400);
        stockService = new StockService(redisOperations, properties);
    }

    @Test
    @DisplayName("预扣库存成功 - 库存充足且未重复秒杀")
    void preDeduct_Success_WhenStockEnough() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        stockService.preDeduct(1L, 2001L, 1001L, 1);
    }

    @Test
    @DisplayName("预扣库存失败 - 库存不足")
    void preDeduct_Fail_WhenStockNotEnough() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(-1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockService.preDeduct(1L, 2001L, 1001L, 1));

        assertEquals(ResponseCodeEnum.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("预扣库存失败 - 重复秒杀")
    void preDeduct_Fail_WhenRepeatSeckill() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(-2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockService.preDeduct(1L, 2001L, 1001L, 1));

        assertEquals(ResponseCodeEnum.REPEAT_SECKILL.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("预扣库存失败 - 库存未预热")
    void preDeduct_Fail_WhenStockNotPreheated() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(-3L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockService.preDeduct(1L, 2001L, 1001L, 1));

        assertEquals(ResponseCodeEnum.STOCK_NOT_PREHEATED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("预扣库存失败 - Redis 返回 null")
    void preDeduct_Fail_WhenRedisReturnsNull() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> stockService.preDeduct(1L, 2001L, 1001L, 1));

        assertEquals(ResponseCodeEnum.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("回滚库存成功 - 恢复库存并清除已秒杀标记")
    void rollback_Success() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        stockService.rollback(1L, 2001L, 1001L, 1);

        verify(redisOperations).execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString(), anyString());
    }
}
