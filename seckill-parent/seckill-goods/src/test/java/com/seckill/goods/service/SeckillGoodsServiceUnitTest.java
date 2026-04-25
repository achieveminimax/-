package com.seckill.goods.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.goods.dto.SeckillActivityDetailResponse;
import com.seckill.goods.dto.SeckillActivityListResponse;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.impl.SeckillGoodsServiceImpl;
import com.seckill.infrastructure.utils.RedisUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillGoodsService 单元测试")
class SeckillGoodsServiceUnitTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SeckillActivity.class);
        TableInfoHelper.initTableInfo(assistant, SeckillGoods.class);
        TableInfoHelper.initTableInfo(assistant, Goods.class);
    }

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private RedisUtils redisUtils;

    private SeckillGoodsServiceImpl seckillGoodsService;

    @BeforeEach
    void setUp() {
        seckillGoodsService = new SeckillGoodsServiceImpl(
                seckillActivityMapper,
                seckillGoodsMapper,
                goodsMapper,
                redisUtils
        );
    }

    @Nested
    @DisplayName("getActivityList 测试")
    class GetActivityListTests {

        @Test
        @DisplayName("秒杀活动列表默认查询 - 返回进行中和即将开始活动，且进行中优先")
        void getActivityList_DefaultStatus_ReturnsOngoingAndUpcoming() {
            LocalDateTime now = LocalDateTime.now();
            SeckillActivity ongoing = activity(1L, "进行中活动", now.minusMinutes(10), now.plusMinutes(20));
            SeckillActivity upcoming = activity(2L, "即将开始活动", now.plusMinutes(30), now.plusHours(1));
            SeckillActivity ended = activity(3L, "已结束活动", now.minusHours(2), now.minusHours(1));

            SeckillGoods ongoingGoods = seckillGoods(1L, 101L, "7999.00", 100, 1, 77);
            SeckillGoods upcomingGoods = seckillGoods(2L, 102L, "5499.00", 200, 1, 0);

            when(redisUtils.get("seckill:activity:list:0:1:10")).thenReturn(null);
            when(redisUtils.pipelineGet(any())).thenReturn(List.of(23));
            when(seckillActivityMapper.selectList(any())).thenReturn(List.of(upcoming, ended, ongoing));
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(ongoingGoods, upcomingGoods));
            when(goodsMapper.selectBatchIds(any())).thenReturn(List.of(goods(101L, "iPhone 16 Pro Max", "9999.00"),
                    goods(102L, "Mate 70 Pro", "6999.00")));

            PageResult<SeckillActivityListResponse> result = seckillGoodsService.getActivityList(null, 1L, 10L);

            assertNotNull(result);
            assertEquals(2L, result.getTotal());
            assertEquals(2, result.getRecords().size());
            assertEquals(ongoing.getId(), result.getRecords().get(0).getActivityId());
            assertEquals(1, result.getRecords().get(0).getStatus());
            assertEquals(23, result.getRecords().get(0).getGoodsList().get(0).getRemainStock());
            assertEquals(upcoming.getId(), result.getRecords().get(1).getActivityId());
            assertEquals(2, result.getRecords().get(1).getStatus());
            assertEquals(200, result.getRecords().get(1).getGoodsList().get(0).getRemainStock());

            verify(redisUtils).set(eq("seckill:activity:list:0:1:10"), eq(result), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("列表缓存命中直接返回（刷新实时库存）")
        void getActivityList_Success_CacheHit() {
            SeckillActivityListResponse cachedActivity = new SeckillActivityListResponse();
            cachedActivity.setActivityId(1L);
            PageResult<SeckillActivityListResponse> cached = PageResult.build(List.of(cachedActivity), 1L, 10L, 1L);
            when(redisUtils.get("seckill:activity:list:0:1:10")).thenReturn(cached);

            PageResult<SeckillActivityListResponse> result = seckillGoodsService.getActivityList(null, 1L, 10L);

            assertNotNull(result);
            verify(seckillActivityMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("无活动返回空结果")
        void getActivityList_EmptyResult() {
            when(redisUtils.get("seckill:activity:list:0:1:10")).thenReturn(null);
            when(seckillActivityMapper.selectList(any())).thenReturn(Collections.emptyList());

            PageResult<SeckillActivityListResponse> result = seckillGoodsService.getActivityList(null, 1L, 10L);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
        }

        @Test
        @DisplayName("按已结束状态筛选")
        void getActivityList_FilterByEndedStatus() {
            LocalDateTime now = LocalDateTime.now();
            SeckillActivity ended = activity(3L, "已结束活动", now.minusHours(2), now.minusHours(1));
            SeckillActivity ongoing = activity(1L, "进行中活动", now.minusMinutes(10), now.plusMinutes(20));

            when(redisUtils.get("seckill:activity:list:3:1:10")).thenReturn(null);
            when(seckillActivityMapper.selectList(any())).thenReturn(List.of(ongoing, ended));
            SeckillGoods endedGoods = seckillGoods(3L, 103L, "3999.00", 50, 1, 50);
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(endedGoods));
            when(goodsMapper.selectBatchIds(any())).thenReturn(List.of(goods(103L, "Old Phone", "4999.00")));
            when(redisUtils.pipelineGet(any())).thenReturn(List.of(0));

            PageResult<SeckillActivityListResponse> result = seckillGoodsService.getActivityList(3, 1L, 10L);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(3, result.getRecords().getFirst().getStatus());
        }
    }

    @Nested
    @DisplayName("getActivityDetail 测试")
    class GetActivityDetailTests {

        @Test
        @DisplayName("活动详情查询成功")
        void getActivityDetail_Success() {
            LocalDateTime now = LocalDateTime.now();
            SeckillActivity act = activity(1L, "限时秒杀", now.minusMinutes(5), now.plusMinutes(30));
            SeckillGoods sg = seckillGoods(1L, 101L, "7999.00", 100, 1, 50);

            when(redisUtils.get("seckill:activity:1")).thenReturn(null);
            when(seckillActivityMapper.selectById(1L)).thenReturn(act);
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(sg));
            when(goodsMapper.selectBatchIds(any())).thenReturn(List.of(goods(101L, "iPhone 16 Pro Max", "9999.00")));
            when(redisUtils.pipelineGet(any())).thenReturn(List.of(50));

            SeckillActivityDetailResponse result = seckillGoodsService.getActivityDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getActivityId());
            assertEquals("限时秒杀", result.getActivityName());
            assertEquals(1, result.getStatus());
            assertEquals("进行中", result.getStatusDesc());
            assertEquals(4, result.getRules().size());
            assertEquals(50, result.getGoodsList().getFirst().getRemainStock());
            verify(redisUtils).set(eq("seckill:activity:1"), eq(result), anyLong(), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("详情缓存命中直接返回")
        void getActivityDetail_Success_CacheHit() {
            SeckillActivityDetailResponse cached = new SeckillActivityDetailResponse();
            cached.setActivityId(1L);
            when(redisUtils.get("seckill:activity:1")).thenReturn(cached);

            SeckillActivityDetailResponse result = seckillGoodsService.getActivityDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getActivityId());
            verify(seckillActivityMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("活动不存在抛异常")
        void getActivityDetail_NotFound() {
            when(redisUtils.get("seckill:activity:999")).thenReturn(null);
            when(seckillActivityMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> seckillGoodsService.getActivityDetail(999L));
            assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    private SeckillActivity activity(Long id, String name, LocalDateTime startTime, LocalDateTime endTime) {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(id);
        activity.setActivityName(name);
        activity.setActivityImg("https://example.com/activity-" + id + ".jpg");
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setStatus(1);
        return activity;
    }

    private SeckillGoods seckillGoods(Long activityId,
                                      Long goodsId,
                                      String seckillPrice,
                                      Integer stock,
                                      Integer limitPerUser,
                                      Integer salesCount) {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(activityId);
        seckillGoods.setGoodsId(goodsId);
        seckillGoods.setSeckillPrice(new BigDecimal(seckillPrice));
        seckillGoods.setSeckillStock(stock);
        seckillGoods.setLimitPerUser(limitPerUser);
        seckillGoods.setSalesCount(salesCount);
        return seckillGoods;
    }

    private Goods goods(Long goodsId, String name, String price) {
        Goods goods = new Goods();
        goods.setId(goodsId);
        goods.setName(name);
        goods.setPrice(new BigDecimal(price));
        goods.setCoverImage("https://example.com/goods-" + goodsId + ".jpg");
        return goods;
    }
}
