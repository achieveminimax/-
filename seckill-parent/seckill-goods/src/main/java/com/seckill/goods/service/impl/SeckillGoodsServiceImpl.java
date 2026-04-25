package com.seckill.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.goods.dto.SeckillActivityDetailResponse;
import com.seckill.goods.dto.SeckillActivityListResponse;
import com.seckill.goods.dto.SeckillGoodsItemResponse;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.SeckillGoodsService;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 秒杀活动只读服务。
 * <p>
 * 这里不负责扣减库存或创建订单，只负责把活动、商品和 Redis 中的实时库存组装成前台可直接使用的视图模型。
 */
@Service
@RequiredArgsConstructor
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    private static final String SECKILL_ACTIVITY_LIST_KEY_PREFIX = "seckill:activity:list:";
    private static final String SECKILL_ACTIVITY_KEY_PREFIX = "seckill:activity:";
    private static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:";
    private static final long ACTIVITY_LIST_BASE_TTL_SECONDS = 60L;
    private static final long ACTIVITY_DETAIL_BASE_TTL_SECONDS = 600L;
    private static final long RANDOM_TTL_SECONDS = 120L;

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
    public PageResult<SeckillActivityListResponse> getActivityList(Integer status, Long current, Long size) {
        long pageNo = normalizeCurrent(current);
        long pageSize = normalizeSize(size);
        String cacheKey = SECKILL_ACTIVITY_LIST_KEY_PREFIX + normalizeStatus(status) + ":" + pageNo + ":" + pageSize;
        PageResult<SeckillActivityListResponse> cached = redisUtils.get(cacheKey);
        if (cached != null) {
            // 列表主体允许走短 TTL 缓存，但库存必须在返回前刷新为 Redis 中的实时值。
            refreshRealtimeStock(cached.getRecords());
            return cached;
        }

        List<SeckillActivity> activities = seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .ne(SeckillActivity::getStatus, 3)
                .orderByAsc(SeckillActivity::getStartTime, SeckillActivity::getId));
        if (activities.isEmpty()) {
            return PageResult.empty();
        }

        List<SeckillActivity> filtered = activities.stream()
                .filter(activity -> matchQueryStatus(resolveQueryStatus(activity), status))
                .sorted(this::compareForDisplay)
                .toList();
        if (filtered.isEmpty()) {
            return PageResult.empty();
        }

        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, filtered.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, filtered.size());
        List<SeckillActivity> pageActivities = filtered.subList(fromIndex, toIndex);

        Map<Long, List<SeckillGoods>> activityGoodsMap = loadActivityGoodsMap(pageActivities.stream()
                .map(SeckillActivity::getId)
                .collect(Collectors.toSet()));
        Map<Long, Goods> goodsMap = loadGoodsMap(activityGoodsMap.values().stream()
                .flatMap(List::stream)
                .map(SeckillGoods::getGoodsId)
                .collect(Collectors.toSet()));

        List<SeckillActivityListResponse> records = pageActivities.stream()
                .map(activity -> toActivityListResponse(activity, activityGoodsMap.getOrDefault(activity.getId(), Collections.emptyList()), goodsMap))
                .toList();
        refreshRealtimeStock(records);

        PageResult<SeckillActivityListResponse> result = PageResult.build(records, (long) filtered.size(), pageSize, pageNo);
        redisUtils.set(cacheKey, result, randomListTtl(), TimeUnit.SECONDS);
        return result;
    }

    @Override
    public SeckillActivityDetailResponse getActivityDetail(Long activityId) {
        SeckillActivityDetailResponse cached = redisUtils.get(SECKILL_ACTIVITY_KEY_PREFIX + activityId);
        if (cached != null) {
            // 详情和列表一样：结构可以缓存，但剩余库存要在响应前实时覆盖。
            refreshRealtimeStock(cached);
            return cached;
        }

        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }

        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
                .orderByAsc(SeckillGoods::getId));
        Map<Long, Goods> goodsMap = loadGoodsMap(seckillGoodsList.stream()
                .map(SeckillGoods::getGoodsId)
                .collect(Collectors.toSet()));

        SeckillActivityDetailResponse response = new SeckillActivityDetailResponse();
        response.setActivityId(activity.getId());
        response.setActivityName(activity.getActivityName());
        response.setActivityImg(activity.getActivityImg());
        response.setDescription(activity.getDescription());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setStatus(resolveQueryStatus(activity));
        response.setStatusDesc(toStatusDesc(response.getStatus()));
        // 规则文案先在后端统一维护，后续如果有活动级规则再替换为数据库配置。
        response.setRules(DEFAULT_RULES);
        response.setGoodsList(seckillGoodsList.stream()
                .map(seckillGoods -> toGoodsItemResponse(seckillGoods, goodsMap.get(seckillGoods.getGoodsId())))
                .toList());
        refreshRealtimeStock(response);

        redisUtils.set(SECKILL_ACTIVITY_KEY_PREFIX + activityId, response, randomDetailTtl(), TimeUnit.SECONDS);
        return response;
    }

    private Map<Long, List<SeckillGoods>> loadActivityGoodsMap(Set<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                        .in(SeckillGoods::getActivityId, activityIds)
                        .orderByAsc(SeckillGoods::getId))
                .stream()
                .collect(Collectors.groupingBy(SeckillGoods::getActivityId));
    }

    private Map<Long, Goods> loadGoodsMap(Set<Long> goodsIds) {
        if (goodsIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return goodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
    }

    private SeckillActivityListResponse toActivityListResponse(SeckillActivity activity,
                                                               List<SeckillGoods> seckillGoodsList,
                                                               Map<Long, Goods> goodsMap) {
        SeckillActivityListResponse response = new SeckillActivityListResponse();
        response.setActivityId(activity.getId());
        response.setActivityName(activity.getActivityName());
        response.setActivityImg(activity.getActivityImg());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setStatus(resolveQueryStatus(activity));
        response.setStatusDesc(toStatusDesc(response.getStatus()));
        response.setGoodsList(seckillGoodsList.stream()
                .map(seckillGoods -> toGoodsItemResponse(seckillGoods, goodsMap.get(seckillGoods.getGoodsId())))
                .toList());
        return response;
    }

    private SeckillGoodsItemResponse toGoodsItemResponse(SeckillGoods seckillGoods, Goods goods) {
        SeckillGoodsItemResponse response = new SeckillGoodsItemResponse();
        response.setGoodsId(seckillGoods.getGoodsId());
        response.setGoodsName(goods == null ? null : goods.getName());
        response.setGoodsImg(goods == null ? null : goods.getCoverImage());
        response.setOriginalPrice(goods == null ? null : goods.getPrice());
        response.setSeckillPrice(seckillGoods.getSeckillPrice());
        response.setTotalStock(seckillGoods.getSeckillStock());
        response.setStock(seckillGoods.getSeckillStock());
        response.setRemainStock(seckillGoods.getSeckillStock());
        response.setLimitPerUser(seckillGoods.getLimitPerUser());
        response.setSalesCount(seckillGoods.getSalesCount());
        return response;
    }

    private void refreshRealtimeStock(List<SeckillActivityListResponse> activities) {
        List<String> allKeys = new java.util.ArrayList<>();
        List<SeckillGoodsItemResponse> allGoods = new java.util.ArrayList<>();
        for (SeckillActivityListResponse activity : activities) {
            if (activity.getGoodsList() == null) {
                continue;
            }
            for (SeckillGoodsItemResponse goods : activity.getGoodsList()) {
                allKeys.add(SECKILL_STOCK_KEY_PREFIX + activity.getActivityId() + ":" + goods.getGoodsId());
                allGoods.add(goods);
            }
        }
        if (allKeys.isEmpty()) {
            return;
        }
        List<Object> cachedStocks = redisUtils.pipelineGet(allKeys);
        for (int i = 0; i < allGoods.size(); i++) {
            Integer cachedStock = cachedStocks.size() > i ? (Integer) cachedStocks.get(i) : null;
            int remain = cachedStock != null ? cachedStock :
                    (allGoods.get(i).getTotalStock() == null ? 0 : allGoods.get(i).getTotalStock());
            allGoods.get(i).setRemainStock(remain);
            allGoods.get(i).setStock(remain);
        }
    }

    private void refreshRealtimeStock(SeckillActivityDetailResponse response) {
        if (response.getGoodsList() == null || response.getGoodsList().isEmpty()) {
            return;
        }
        List<String> keys = response.getGoodsList().stream()
                .map(g -> SECKILL_STOCK_KEY_PREFIX + response.getActivityId() + ":" + g.getGoodsId())
                .toList();
        List<Object> cachedStocks = redisUtils.pipelineGet(keys);
        for (int i = 0; i < response.getGoodsList().size(); i++) {
            Integer cachedStock = cachedStocks.size() > i ? (Integer) cachedStocks.get(i) : null;
            SeckillGoodsItemResponse goods = response.getGoodsList().get(i);
            int remain = cachedStock != null ? cachedStock :
                    (goods.getTotalStock() == null ? 0 : goods.getTotalStock());
            goods.setRemainStock(remain);
            goods.setStock(remain);
        }
    }

    private int getRemainStock(Long activityId, SeckillGoodsItemResponse goods) {
        Integer cachedStock = redisUtils.get(SECKILL_STOCK_KEY_PREFIX + activityId + ":" + goods.getGoodsId());
        if (cachedStock != null) {
            return cachedStock;
        }
        // 预热数据尚未写入 Redis 时回退到活动配置库存，保证接口可用但不是最终并发控制依据。
        return goods.getTotalStock() == null ? 0 : goods.getTotalStock();
    }

    private int resolveQueryStatus(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && activity.getEndTime().isBefore(now)) {
            return 3;
        }
        if (activity.getStartTime() != null && activity.getStartTime().isAfter(now)) {
            return 2;
        }
        return 1;
    }

    private boolean matchQueryStatus(int activityStatus, Integer queryStatus) {
        Integer normalized = normalizeStatus(queryStatus);
        if (normalized == 0) {
            // API 约定“全部”并不包含已结束活动，默认只展示用户仍可能关心的活动。
            return activityStatus == 1 || activityStatus == 2;
        }
        return activityStatus == normalized;
    }

    private int compareForDisplay(SeckillActivity left, SeckillActivity right) {
        int leftStatus = resolveQueryStatus(left);
        int rightStatus = resolveQueryStatus(right);
        if (leftStatus != rightStatus) {
            // 状态值本身就代表展示优先级：进行中 -> 即将开始 -> 已结束。
            return Integer.compare(leftStatus, rightStatus);
        }
        if (leftStatus == 1) {
            // 进行中的活动按开始时间倒序，越接近当前热点的活动越靠前。
            return right.getStartTime().compareTo(left.getStartTime());
        }
        // 即将开始的活动按开始时间升序，方便前台从最近一场开始倒计时。
        return Comparator.comparing(SeckillActivity::getStartTime).compare(left, right);
    }

    private String toStatusDesc(Integer status) {
        return switch (status) {
            case 1 -> "进行中";
            case 2 -> "即将开始";
            case 3 -> "已结束";
            default -> "未知";
        };
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null || status < 0 || status > 3) {
            return 0;
        }
        return status;
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, 100L);
    }

    private long randomListTtl() {
        return ACTIVITY_LIST_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1);
    }

    private long randomDetailTtl() {
        return ACTIVITY_DETAIL_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS + 1);
    }
}
