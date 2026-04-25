package com.seckill.seckill.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.seckill.config.SeckillProperties;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillPathService 单元测试")
class SeckillPathServiceUnitTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SeckillGoods.class);
    }

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private RedisOperations redisOperations;

    private SeckillPathService seckillPathService;

    @BeforeEach
    void setUp() {
        SeckillProperties properties = new SeckillProperties();
        properties.setPathTtlSeconds(300);
        properties.setPathMaxAcquireCount(3);

        seckillPathService = new SeckillPathService(
                seckillActivityMapper,
                seckillGoodsMapper,
                redisUtils,
                redisOperations,
                properties
        );
    }

    @Test
    @DisplayName("获取秒杀地址成功 - 活动开始前五分钟内允许获取")
    void createPath_Success_WhenWithinAcquireWindow() {
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusMinutes(10)));
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());
        when(redisOperations.increment(anyString())).thenReturn(1L);
        when(redisOperations.expire(anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);

        var response = seckillPathService.createPath(1001L, 1L, 2001L);

        assertNotNull(response);
        assertNotNull(response.getExpiresAt());
        assertTrue(response.getSeckillPath().startsWith("/"));
        verify(redisUtils).set(anyString(), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("获取秒杀地址失败 - 活动开始前超过五分钟")
    void createPath_Fail_WhenBeforeAcquireWindow() {
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().plusMinutes(6), LocalDateTime.now().plusMinutes(20)));
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.createPath(1001L, 1L, 2001L));

        assertEquals(ResponseCodeEnum.SECKILL_NOT_START.getCode(), exception.getCode());
        verify(redisUtils, never()).set(anyString(), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("校验秒杀地址失败 - 路径无效")
    void validatePath_Fail_WhenInvalidPath() {
        when(redisUtils.get(anyString())).thenReturn("expected-path");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.validatePath(1001L, 1L, 2001L, "/wrong-path"));

        assertEquals(ResponseCodeEnum.SECKILL_PATH_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("校验秒杀地址成功 - 路径匹配")
    void validatePath_Success_WhenPathMatches() {
        when(redisUtils.get(anyString())).thenReturn("valid-path");

        assertDoesNotThrow(() -> seckillPathService.validatePath(1001L, 1L, 2001L, "/valid-path"));
    }

    @Test
    @DisplayName("校验秒杀地址失败 - 路径在Redis中不存在")
    void validatePath_Fail_WhenPathExpired() {
        when(redisUtils.get(anyString())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.validatePath(1001L, 1L, 2001L, "/some-path"));

        assertEquals(ResponseCodeEnum.SECKILL_PATH_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("requireActivity - 活动不存在抛异常")
    void requireActivity_NotFound() {
        when(seckillActivityMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.requireActivity(999L));

        assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("requireSeckillGoods - 秒杀商品不存在抛异常")
    void requireSeckillGoods_NotFound() {
        when(seckillGoodsMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.requireSeckillGoods(1L, 999L));

        assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("validateExecuteWindow - 活动未开始抛异常")
    void validateExecuteWindow_NotStart() {
        SeckillActivity activity = activity(LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusMinutes(20));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.validateExecuteWindow(activity));

        assertEquals(ResponseCodeEnum.SECKILL_NOT_START.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("validateExecuteWindow - 活动已结束抛异常")
    void validateExecuteWindow_Ended() {
        SeckillActivity activity = activity(LocalDateTime.now().minusMinutes(20), LocalDateTime.now().minusMinutes(10));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.validateExecuteWindow(activity));

        assertEquals(ResponseCodeEnum.SECKILL_ENDED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("validateExecuteWindow - 活动进行中通过")
    void validateExecuteWindow_Ongoing() {
        SeckillActivity activity = activity(LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusMinutes(10));

        assertDoesNotThrow(() -> seckillPathService.validateExecuteWindow(activity));
    }

    @Test
    @DisplayName("createPath - 获取次数超过限制抛异常")
    void createPath_Fail_MaxAcquireExceeded() {
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().plusMinutes(1), LocalDateTime.now().plusMinutes(10)));
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());
        when(redisOperations.increment(anyString())).thenReturn(4L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.createPath(1001L, 1L, 2001L));

        assertEquals(ResponseCodeEnum.RATE_LIMIT.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createPath - 活动已结束抛异常")
    void createPath_Fail_ActivityEnded() {
        when(seckillActivityMapper.selectById(1L)).thenReturn(activity(LocalDateTime.now().minusMinutes(20), LocalDateTime.now().minusMinutes(10)));
        when(seckillGoodsMapper.selectOne(any())).thenReturn(seckillGoods());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillPathService.createPath(1001L, 1L, 2001L));

        assertEquals(ResponseCodeEnum.SECKILL_ENDED.getCode(), exception.getCode());
    }

    private SeckillActivity activity(LocalDateTime startTime, LocalDateTime endTime) {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setActivityName("测试活动");
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setStatus(1);
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
