package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.seckill.annotation.RateLimitScene;
import com.seckill.seckill.config.SeckillProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 单元测试")
class RateLimitServiceUnitTest {

    @Mock
    private RedisOperations redisOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        SeckillProperties properties = new SeckillProperties();
        properties.setRateLimitPerUser(1);
        properties.setRateLimitGlobal(5000);
        rateLimitService = new RateLimitService(redisOperations, properties);
    }

    @Test
    @DisplayName("限流检查通过 - 用户频率在阈值内")
    void check_Success_WhenUserRateWithinLimit() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(1L);

        rateLimitService.check(RateLimitScene.SECKILL_PATH, 1001L, "/api/seckill/path/1");
    }

    @Test
    @DisplayName("限流检查失败 - 用户频率超过阈值")
    void check_Fail_WhenUserRateExceedsLimit() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> rateLimitService.check(RateLimitScene.SECKILL_PATH, 1001L, "/api/seckill/path/1"));

        assertEquals(ResponseCodeEnum.RATE_LIMIT.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("限流检查失败 - 未登录用户")
    void check_Fail_WhenUserNotLoggedIn() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> rateLimitService.check(RateLimitScene.SECKILL_PATH, null, "/api/seckill/path/1"));

        assertEquals(ResponseCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("秒杀执行限流检查 - 全局和用户限流都通过")
    void check_SeckillExecute_Success() {
        when(redisOperations.execute(any(DefaultRedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(1L);

        rateLimitService.check(RateLimitScene.SECKILL_EXECUTE, 1001L, "/api/seckill/do");
    }
}
