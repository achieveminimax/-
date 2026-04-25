package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.seckill.config.SeckillProperties;
import com.seckill.seckill.support.SeckillRedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    private final RedisOperations redisOperations;
    private final SeckillProperties seckillProperties;

    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = createPreDeductScript();
    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = createRollbackScript();

    @Autowired
    public StockService(RedisOperations redisOperations, SeckillProperties seckillProperties) {
        this.redisOperations = redisOperations;
        this.seckillProperties = seckillProperties;
    }

    public void preDeduct(Long activityId, Long goodsId, Long userId, int quantity) {
        Long result = redisOperations.execute(
                PRE_DEDUCT_SCRIPT,
                List.of(SeckillRedisKeys.stock(activityId, goodsId), SeckillRedisKeys.done(activityId, goodsId)),
                String.valueOf(quantity),
                String.valueOf(userId),
                String.valueOf(seckillProperties.getDoneTtlSeconds())
        );
        if (result == null || result == -1L) {
            throw new BusinessException(ResponseCodeEnum.STOCK_NOT_ENOUGH);
        }
        if (result == -2L) {
            throw new BusinessException(ResponseCodeEnum.REPEAT_SECKILL);
        }
        if (result == -3L) {
            throw new BusinessException(ResponseCodeEnum.STOCK_NOT_PREHEATED);
        }
    }

    public void rollback(Long activityId, Long goodsId, Long userId, int quantity) {
        redisOperations.execute(
                ROLLBACK_SCRIPT,
                List.of(SeckillRedisKeys.stock(activityId, goodsId), SeckillRedisKeys.done(activityId, goodsId)),
                String.valueOf(quantity),
                String.valueOf(userId),
                String.valueOf(seckillProperties.getDoneTtlSeconds())
        );
    }

    private static DefaultRedisScript<Long> createPreDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/pre_deduct_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    private static DefaultRedisScript<Long> createRollbackScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/rollback_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
