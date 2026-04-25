package com.seckill.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.dto.SeckillActivityDetailResponse;
import com.seckill.goods.dto.SeckillGoodsItemResponse;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.SeckillPreheatService;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀活动预热服务。
 * <p>
 * 预热的目标不是提前执行秒杀逻辑，而是把活动详情和可扣减库存先装载到 Redis，
 * 这样真正开始抢购时，读流量和库存判断都能优先落在缓存层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPreheatServiceImpl implements SeckillPreheatService {

    private static final String SECKILL_ACTIVITY_KEY_PREFIX = "seckill:activity:";
    private static final String SECKILL_ACTIVITY_LIST_KEY_PATTERN = "seckill:activity:list:*";
    private static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:";

    private static final List<String> DEFAULT_RULES = List.of(
            "每人限购 1 件",
            "秒杀商品不支持使用优惠券",
            "秒杀订单需在 15 分钟内完成支付",
            "秒杀成功后请尽快完成支付"
    );

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final GoodsMapper goodsMapper;
    private final RedisUtils redisUtils;

    @Override
    public int preheatUpcomingActivities() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime preheatDeadline = now.plusMinutes(5);
        // 只扫描未来 5 分钟内即将开始、且未被下架的活动，避免无意义预热占用缓存空间。
        List<SeckillActivity> activities = seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .ne(SeckillActivity::getStatus, 3)
                .ge(SeckillActivity::getEndTime, now)
                .between(SeckillActivity::getStartTime, now, preheatDeadline)
                .orderByAsc(SeckillActivity::getStartTime));

        for (SeckillActivity activity : activities) {
            preheatActivity(activity.getId());
        }
        return activities.size();
    }

    @Override
    public void preheatActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }
        if (activity.getStatus() != null && activity.getStatus() == 3) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "已下架活动不允许预热");
        }

        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
                .orderByAsc(SeckillGoods::getId));
        if (seckillGoodsList.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "活动下不存在秒杀商品");
        }

        Map<Long, Goods> goodsMap = loadGoodsMap(seckillGoodsList.stream()
                .map(SeckillGoods::getGoodsId)
                .collect(Collectors.toSet()));
        // 预热数据至少保留到活动结束后一段时间，给详情页和问题排查预留缓冲窗口。
        long ttlSeconds = buildPreheatTtlSeconds(activity.getEndTime());

        Map<String, Object> stockKvMap = new java.util.LinkedHashMap<>();
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            stockKvMap.put(SECKILL_STOCK_KEY_PREFIX + activityId + ":" + seckillGoods.getGoodsId(),
                    seckillGoods.getSeckillStock());
        }
        redisUtils.pipelineSet(stockKvMap, ttlSeconds, TimeUnit.SECONDS);

        // 同步预热活动详情，减少活动开始后第一波详情请求直接打到数据库。
        SeckillActivityDetailResponse detailResponse = buildActivityDetail(activity, seckillGoodsList, goodsMap);
        redisUtils.set(SECKILL_ACTIVITY_KEY_PREFIX + activityId, detailResponse, ttlSeconds, TimeUnit.SECONDS);
        // 活动列表依赖活动详情和库存汇总，单个活动预热后需要让列表缓存统一失效重建。
        redisUtils.deleteByPattern(SECKILL_ACTIVITY_LIST_KEY_PATTERN);

        log.info("秒杀活动预热完成, activityId={}, goodsCount={}", activityId, seckillGoodsList.size());
    }

    private Map<Long, Goods> loadGoodsMap(Set<Long> goodsIds) {
        if (goodsIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return goodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
    }

    private SeckillActivityDetailResponse buildActivityDetail(SeckillActivity activity,
                                                              List<SeckillGoods> seckillGoodsList,
                                                              Map<Long, Goods> goodsMap) {
        SeckillActivityDetailResponse response = new SeckillActivityDetailResponse();
        response.setActivityId(activity.getId());
        response.setActivityName(activity.getActivityName());
        response.setActivityImg(activity.getActivityImg());
        response.setDescription(activity.getDescription());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setStatus(resolveStatus(activity));
        response.setStatusDesc(toStatusDesc(response.getStatus()));
        response.setRules(DEFAULT_RULES);
        response.setGoodsList(seckillGoodsList.stream()
                .map(seckillGoods -> toGoodsItem(seckillGoods, goodsMap.get(seckillGoods.getGoodsId())))
                .toList());
        return response;
    }

    private SeckillGoodsItemResponse toGoodsItem(SeckillGoods seckillGoods, Goods goods) {
        SeckillGoodsItemResponse response = new SeckillGoodsItemResponse();
        response.setGoodsId(seckillGoods.getGoodsId());
        response.setGoodsName(goods == null ? null : goods.getName());
        response.setGoodsImg(goods == null ? null : goods.getCoverImage());
        response.setOriginalPrice(goods == null ? null : goods.getPrice());
        response.setSeckillPrice(seckillGoods.getSeckillPrice());
        response.setTotalStock(seckillGoods.getSeckillStock());
        response.setRemainStock(seckillGoods.getSeckillStock());
        response.setStock(seckillGoods.getSeckillStock());
        response.setLimitPerUser(seckillGoods.getLimitPerUser());
        response.setSalesCount(seckillGoods.getSalesCount());
        return response;
    }

    private int resolveStatus(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && activity.getEndTime().isBefore(now)) {
            return 3;
        }
        if (activity.getStartTime() != null && activity.getStartTime().isAfter(now)) {
            return 2;
        }
        return 1;
    }

    private String toStatusDesc(Integer status) {
        return switch (status) {
            case 1 -> "进行中";
            case 2 -> "即将开始";
            case 3 -> "已结束";
            default -> "未知";
        };
    }

    private long buildPreheatTtlSeconds(LocalDateTime endTime) {
        if (endTime == null) {
            return 3600L;
        }
        // 统一在活动结束后再多保留 1 小时，兼顾详情页查询和运营排查。
        long seconds = Duration.between(LocalDateTime.now(), endTime.plusHours(1)).getSeconds();
        return Math.max(seconds, 600L);
    }
}
