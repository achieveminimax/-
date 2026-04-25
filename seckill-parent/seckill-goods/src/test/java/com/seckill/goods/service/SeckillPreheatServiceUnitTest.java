package com.seckill.goods.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.impl.SeckillPreheatServiceImpl;
import com.seckill.infrastructure.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillPreheatService 单元测试")
class SeckillPreheatServiceUnitTest {

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private RedisUtils redisUtils;

    private SeckillPreheatServiceImpl seckillPreheatService;

    @BeforeEach
    void setUp() {
        seckillPreheatService = new SeckillPreheatServiceImpl(seckillActivityMapper, seckillGoodsMapper, goodsMapper, redisUtils);
    }

    @Nested
    @DisplayName("preheatActivity 测试")
    class PreheatActivityTests {

        @Test
        @DisplayName("预热成功 - Pipeline写入库存与活动缓存")
        void preheatActivity_Success_WriteRedisKeys() {
            SeckillActivity activity = activity(1L, 0);
            SeckillGoods seckillGoods = seckillGoods(1L, 1L);
            Goods goods = goods(1L);

            when(seckillActivityMapper.selectById(1L)).thenReturn(activity);
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(seckillGoods));
            when(goodsMapper.selectBatchIds(any())).thenReturn(List.of(goods));

            seckillPreheatService.preheatActivity(1L);

            verify(redisUtils).pipelineSet(any(Map.class), anyLong(), eq(TimeUnit.SECONDS));
            verify(redisUtils).set(eq("seckill:activity:1"), any(), anyLong(), eq(TimeUnit.SECONDS));
            verify(redisUtils).deleteByPattern("seckill:activity:list:*");
        }

        @Test
        @DisplayName("预热失败 - 活动不存在")
        void preheatActivity_Fail_WhenActivityMissing() {
            when(seckillActivityMapper.selectById(99L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, () -> seckillPreheatService.preheatActivity(99L));

            assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("预热失败 - 已下架活动不允许预热")
        void preheatActivity_Fail_ActivityOffline() {
            SeckillActivity activity = activity(1L, 3);

            when(seckillActivityMapper.selectById(1L)).thenReturn(activity);

            BusinessException exception = assertThrows(BusinessException.class, () -> seckillPreheatService.preheatActivity(1L));

            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("已下架"));
        }

        @Test
        @DisplayName("预热失败 - 活动下无商品")
        void preheatActivity_Fail_NoGoods() {
            SeckillActivity activity = activity(1L, 0);

            when(seckillActivityMapper.selectById(1L)).thenReturn(activity);
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());

            BusinessException exception = assertThrows(BusinessException.class, () -> seckillPreheatService.preheatActivity(1L));

            assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("秒杀商品"));
        }
    }

    @Nested
    @DisplayName("preheatUpcomingActivities 测试")
    class PreheatUpcomingActivitiesTests {

        @Test
        @DisplayName("预热未来5分钟内的活动")
        void preheatUpcomingActivities_Success() {
            SeckillActivity upcoming = activity(1L, 0);
            upcoming.setStartTime(LocalDateTime.now().plusMinutes(2));
            upcoming.setEndTime(LocalDateTime.now().plusHours(1));
            SeckillGoods sg = seckillGoods(1L, 1L);
            Goods g = goods(1L);

            when(seckillActivityMapper.selectList(any())).thenReturn(List.of(upcoming));
            when(seckillActivityMapper.selectById(1L)).thenReturn(upcoming);
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(sg));
            when(goodsMapper.selectBatchIds(any())).thenReturn(List.of(g));

            int count = seckillPreheatService.preheatUpcomingActivities();

            assertEquals(1, count);
            verify(redisUtils).pipelineSet(any(Map.class), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("无即将开始的活动")
        void preheatUpcomingActivities_NoUpcoming() {
            when(seckillActivityMapper.selectList(any())).thenReturn(Collections.emptyList());

            int count = seckillPreheatService.preheatUpcomingActivities();

            assertEquals(0, count);
        }
    }

    private SeckillActivity activity(Long id, int status) {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(id);
        activity.setActivityName("即将开始活动");
        activity.setStartTime(LocalDateTime.now().plusMinutes(3));
        activity.setEndTime(LocalDateTime.now().plusHours(1));
        activity.setStatus(status);
        return activity;
    }

    private SeckillGoods seckillGoods(Long activityId, Long goodsId) {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(activityId);
        seckillGoods.setGoodsId(goodsId);
        seckillGoods.setSeckillPrice(new BigDecimal("9.90"));
        seckillGoods.setSeckillStock(50);
        seckillGoods.setLimitPerUser(1);
        return seckillGoods;
    }

    private Goods goods(Long id) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("秒杀商品");
        goods.setPrice(new BigDecimal("19.90"));
        goods.setCoverImage("https://example.com/goods.jpg");
        return goods;
    }
}
