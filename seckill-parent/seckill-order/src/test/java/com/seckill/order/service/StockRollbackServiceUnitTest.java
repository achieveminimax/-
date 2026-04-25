package com.seckill.order.service;

import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.service.impl.StockRollbackServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockRollbackServiceImpl 单元测试")
class StockRollbackServiceImplUnitTest {

    @Mock
    private RedisOperations redisOperations;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    private StockRollbackServiceImpl stockRollbackService;

    @BeforeEach
    void setUp() {
        stockRollbackService = new StockRollbackServiceImpl(redisOperations, goodsMapper, seckillGoodsMapper);
    }

    @Test
    @DisplayName("库存回滚成功 - Redis原子回滚 + 数据库回滚")
    void rollbackStock_Success() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(1L);
        when(goodsMapper.rollbackStock(anyLong(), anyInt())).thenReturn(1);
        when(seckillGoodsMapper.rollbackSalesCount(anyLong(), anyLong(), anyInt())).thenReturn(1);

        stockRollbackService.rollbackStock(1L, 1001L, 2001L, 1);

        verify(redisOperations).execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any());
        verify(goodsMapper).rollbackStock(1001L, 1);
        verify(seckillGoodsMapper).rollbackSalesCount(1L, 1001L, 1);
    }

    @Test
    @DisplayName("库存回滚跳过 - activityId为null")
    void rollbackStock_Skip_WhenActivityIdNull() {
        stockRollbackService.rollbackStock(null, 1001L, 2001L, 1);

        verify(redisOperations, org.mockito.Mockito.never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("库存回滚跳过 - goodsId为null")
    void rollbackStock_Skip_WhenGoodsIdNull() {
        stockRollbackService.rollbackStock(1L, null, 2001L, 1);

        verify(redisOperations, org.mockito.Mockito.never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("库存回滚跳过 - userId为null")
    void rollbackStock_Skip_WhenUserIdNull() {
        stockRollbackService.rollbackStock(1L, 1001L, null, 1);

        verify(redisOperations, org.mockito.Mockito.never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("库存回滚失败 - Redis异常时向上抛出")
    void rollbackStock_Fail_WhenRedisError() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Redis连接失败"));

        assertThrows(RuntimeException.class,
                () -> stockRollbackService.rollbackStock(1L, 1001L, 2001L, 1));
    }

    @Test
    @DisplayName("库存回滚失败 - 数据库库存回滚异常时向上抛出")
    void rollbackStock_Fail_WhenDbRollbackError() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(1L);
        doThrow(new RuntimeException("数据库异常")).when(goodsMapper).rollbackStock(anyLong(), anyInt());

        assertThrows(RuntimeException.class,
                () -> stockRollbackService.rollbackStock(1L, 1001L, 2001L, 1));
    }

    @Test
    @DisplayName("库存回滚失败 - 秒杀销量回滚异常时向上抛出")
    void rollbackStock_Fail_WhenSeckillSalesRollbackError() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(1L);
        when(goodsMapper.rollbackStock(anyLong(), anyInt())).thenReturn(1);
        doThrow(new RuntimeException("数据库异常")).when(seckillGoodsMapper).rollbackSalesCount(anyLong(), anyLong(), anyInt());

        assertThrows(RuntimeException.class,
                () -> stockRollbackService.rollbackStock(1L, 1001L, 2001L, 1));
    }

    @Test
    @DisplayName("库存回滚 - 多数量回滚成功")
    void rollbackStock_Success_MultipleQuantity() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), any(), any(), any()))
                .thenReturn(1L);
        when(goodsMapper.rollbackStock(anyLong(), anyInt())).thenReturn(1);
        when(seckillGoodsMapper.rollbackSalesCount(anyLong(), anyLong(), anyInt())).thenReturn(1);

        assertDoesNotThrow(() -> stockRollbackService.rollbackStock(1L, 1001L, 2001L, 5));

        verify(goodsMapper).rollbackStock(1001L, 5);
        verify(seckillGoodsMapper).rollbackSalesCount(1L, 1001L, 5);
    }
}
