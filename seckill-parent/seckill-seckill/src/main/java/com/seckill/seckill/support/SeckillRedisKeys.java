package com.seckill.seckill.support;

import com.seckill.common.constant.RedisKeyConstant;

public final class SeckillRedisKeys {

    private SeckillRedisKeys() {
    }

    public static String stock(Long activityId, Long goodsId) {
        return RedisKeyConstant.SECKILL_STOCK + activityId + ":" + goodsId;
    }

    public static String path(Long userId, Long activityId, Long goodsId) {
        return RedisKeyConstant.SECKILL_PATH + userId + ":" + activityId + ":" + goodsId;
    }

    public static String pathAcquireCount(Long userId, Long activityId, Long goodsId) {
        return RedisKeyConstant.SECKILL_PATH_COUNT + userId + ":" + activityId + ":" + goodsId;
    }

    public static String done(Long activityId, Long goodsId) {
        return RedisKeyConstant.SECKILL_DONE + activityId + ":" + goodsId;
    }

    public static String result(Long recordId) {
        return RedisKeyConstant.SECKILL_RESULT + recordId;
    }

    public static String userRateLimit(String uri, Long userId, long epochSecond) {
        return RedisKeyConstant.RATE_LIMIT + uri + ":" + userId + ":" + epochSecond;
    }

    public static String globalRateLimit(String uri, long epochSecond) {
        return RedisKeyConstant.RATE_LIMIT_GLOBAL + uri + ":" + epochSecond;
    }
}
