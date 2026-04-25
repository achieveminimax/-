package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.seckill.annotation.RateLimitScene;
import com.seckill.seckill.config.SeckillProperties;
import com.seckill.seckill.support.SeckillRedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RateLimitService {

    private final RedisOperations redisOperations;
    private final SeckillProperties seckillProperties;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = createScript();

    @Autowired
    public RateLimitService(RedisOperations redisOperations, SeckillProperties seckillProperties) {
        this.redisOperations = redisOperations;
        this.seckillProperties = seckillProperties;
    }

    public void check(RateLimitScene scene, Long userId, String uri) {
        long epochSecond = System.currentTimeMillis() / 1000;
        switch (scene) {
            case SECKILL_PATH -> checkUserLimit(uri, userId, epochSecond, seckillProperties.getRateLimitPerUser(), ResponseCodeEnum.RATE_LIMIT);
            case SECKILL_EXECUTE -> {
                checkGlobalLimit(uri, epochSecond, seckillProperties.getRateLimitGlobal(), ResponseCodeEnum.SECKILL_RATE_LIMIT);
                checkUserLimit(uri, userId, epochSecond, seckillProperties.getRateLimitPerUser(), ResponseCodeEnum.SECKILL_RATE_LIMIT);
            }
            default -> {
            }
        }
    }

    private void checkUserLimit(String uri, Long userId, long epochSecond, int limit, ResponseCodeEnum responseCode) {
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "请先登录");
        }
        String key = SeckillRedisKeys.userRateLimit(uri, userId, epochSecond);
        if (!tryAcquire(key, limit)) {
            throw new BusinessException(responseCode);
        }
    }

    private void checkGlobalLimit(String uri, long epochSecond, int limit, ResponseCodeEnum responseCode) {
        String key = SeckillRedisKeys.globalRateLimit(uri, epochSecond);
        if (!tryAcquire(key, limit)) {
            throw new BusinessException(responseCode);
        }
    }

    private boolean tryAcquire(String key, int limit) {
        Long result = redisOperations.execute(RATE_LIMIT_SCRIPT, Collections.singletonList(key), String.valueOf(limit), "1");
        return result != null && result == 1L;
    }

    private static DefaultRedisScript<Long> createScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/rate_limit.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
