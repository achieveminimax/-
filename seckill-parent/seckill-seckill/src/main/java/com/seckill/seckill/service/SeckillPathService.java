package com.seckill.seckill.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.seckill.config.SeckillProperties;
import com.seckill.seckill.dto.SeckillPathResponse;
import com.seckill.seckill.support.SeckillRedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillPathService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisUtils redisUtils;
    private final RedisOperations redisOperations;
    private final SeckillProperties seckillProperties;

    @Autowired
    public SeckillPathService(SeckillActivityMapper seckillActivityMapper,
                              SeckillGoodsMapper seckillGoodsMapper,
                              RedisUtils redisUtils,
                              RedisOperations redisOperations,
                              SeckillProperties seckillProperties) {
        this.seckillActivityMapper = seckillActivityMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.redisUtils = redisUtils;
        this.redisOperations = redisOperations;
        this.seckillProperties = seckillProperties;
    }

    public SeckillPathResponse createPath(Long userId, Long activityId, Long goodsId) {
        SeckillActivity activity = requireActivity(activityId);
        requireSeckillGoods(activityId, goodsId);
        validatePathWindow(activity);

        String countKey = SeckillRedisKeys.pathAcquireCount(userId, activityId, goodsId);
        Long count = redisOperations.increment(countKey);
        if (count != null && count == 1L) {
            redisOperations.expire(countKey, seckillProperties.getPathTtlSeconds(), TimeUnit.SECONDS);
        }
        if (count != null && count > seckillProperties.getPathMaxAcquireCount()) {
            throw new BusinessException(ResponseCodeEnum.RATE_LIMIT, "秒杀地址获取次数过多，请稍后重试");
        }

        String rawPath = DigestUtil.md5Hex(userId + ":" + activityId + ":" + goodsId + ":" + System.nanoTime());
        redisUtils.set(SeckillRedisKeys.path(userId, activityId, goodsId), rawPath, seckillProperties.getPathTtlSeconds(), TimeUnit.SECONDS);

        SeckillPathResponse response = new SeckillPathResponse();
        response.setSeckillPath("/" + rawPath);
        response.setExpiresAt(LocalDateTime.now().plusSeconds(seckillProperties.getPathTtlSeconds()).format(DATE_TIME_FORMATTER));
        return response;
    }

    public void validatePath(Long userId, Long activityId, Long goodsId, String seckillPath) {
        String expected = redisUtils.get(SeckillRedisKeys.path(userId, activityId, goodsId));
        String normalized = normalizePath(seckillPath);
        if (expected == null || !expected.equals(normalized)) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_PATH_INVALID);
        }
    }

    public SeckillActivity requireActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }
        return activity;
    }

    public SeckillGoods requireSeckillGoods(Long activityId, Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsMapper.selectOne(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
                .eq(SeckillGoods::getGoodsId, goodsId)
                .last("LIMIT 1"));
        if (seckillGoods == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND, "秒杀商品不存在");
        }
        return seckillGoods;
    }

    public void validateExecuteWindow(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_START);
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_ENDED);
        }
    }

    private void validatePathWindow(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = activity.getStartTime();
        LocalDateTime end = activity.getEndTime();
        if (start != null && now.isBefore(start.minusMinutes(5))) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_START);
        }
        if (end != null && now.isAfter(end)) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_ENDED);
        }
    }

    private String normalizePath(String seckillPath) {
        return seckillPath == null ? null : seckillPath.replaceFirst("^/+", "");
    }
}
