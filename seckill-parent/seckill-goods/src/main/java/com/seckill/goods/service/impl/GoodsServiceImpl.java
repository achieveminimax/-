package com.seckill.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.common.utils.JsonUtils;
import com.seckill.goods.dto.AdminGoodsListResponse;
import com.seckill.goods.dto.AdminGoodsSaveRequest;
import com.seckill.goods.dto.GoodsDetailResponse;
import com.seckill.goods.dto.GoodsListResponse;
import com.seckill.goods.entity.Category;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.CategoryMapper;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.goods.service.GoodsService;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品领域核心实现。
 * <p>
 * 这里同时承载了前台商品查询和管理端商品维护两类能力：
 * <ul>
 *     <li>前台读接口优先考虑缓存命中率和接口响应速度</li>
 *     <li>管理端写接口优先保证数据正确性，并在写库后统一清理相关缓存</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private static final String GOODS_LIST_KEY_PREFIX = "goods:list:";
    private static final String GOODS_LIST_KEY_PATTERN = "goods:list:*";
    private static final String GOODS_DETAIL_KEY_PREFIX = "goods:detail:";
    private static final String GOODS_NULL_KEY_PREFIX = "goods:null:";
    private static final String SECKILL_ACTIVITY_KEY_PREFIX = "seckill:activity:";
    private static final String SECKILL_ACTIVITY_LIST_KEY_PATTERN = "seckill:activity:list:*";
    private static final long GOODS_LIST_BASE_TTL_SECONDS = 300L;
    private static final long GOODS_DETAIL_BASE_TTL_SECONDS = 1800L;
    private static final long GOODS_RANDOM_TTL_SECONDS = 300L;
    private static final long GOODS_NULL_TTL_SECONDS = 300L;
    private static final int MAX_PAGE_SIZE = 100;

    private final GoodsMapper goodsMapper;
    private final CategoryMapper categoryMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<GoodsListResponse> getPublicGoodsList(Long categoryId, String keyword, String sort, Long current, Long size) {
        long pageNo = normalizeCurrent(current);
        long pageSize = normalizeSize(size);
        String cacheKey = buildGoodsListCacheKey(categoryId, keyword, sort, pageNo, pageSize);
        PageResult<GoodsListResponse> cached = redisUtils.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 一级分类需要自动展开到全部子分类，这样前台按大类筛选时不需要额外做树遍历。
        List<Long> categoryIds = resolveCategoryIds(categoryId);
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1);
        if (!categoryIds.isEmpty()) {
            wrapper.in(Goods::getCategoryId, categoryIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Goods::getName, keyword.trim())
                    .or()
                    .like(Goods::getDescription, keyword.trim()));
        }
        // 排序规则和 API 文档保持一致，未知排序值会回退到默认排序，避免前端传错参数导致 SQL 混乱。
        applyGoodsSort(wrapper, sort);

        Page<Goods> page = goodsMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, Category> categoryMap = loadCategoryMap(page.getRecords().stream()
                .map(Goods::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, SeckillGoods> ongoingSeckillMap = loadOngoingSeckillGoodsMap(page.getRecords().stream()
                .map(Goods::getId)
                .collect(Collectors.toSet()));

        List<GoodsListResponse> records = page.getRecords().stream()
                .map(goods -> toPublicListResponse(goods, categoryMap.get(goods.getCategoryId()), ongoingSeckillMap.get(goods.getId())))
                .toList();

        PageResult<GoodsListResponse> result = PageResult.build(records, page.getTotal(), page.getSize(), page.getCurrent());
        redisUtils.set(cacheKey, result, randomGoodsListTtl(), TimeUnit.SECONDS);
        return result;
    }

    @Override
    public GoodsDetailResponse getPublicGoodsDetail(Long goodsId) {
        GoodsDetailResponse cached = redisUtils.get(GOODS_DETAIL_KEY_PREFIX + goodsId);
        if (cached != null) {
            return cached;
        }
        // 负缓存用于拦住重复查询不存在商品，避免热点脏 ID 持续打到数据库。
        if (Boolean.TRUE.equals(redisUtils.hasKey(GOODS_NULL_KEY_PREFIX + goodsId))) {
            throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
        }

        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() == 0) {
            redisUtils.set(GOODS_NULL_KEY_PREFIX + goodsId, "1", GOODS_NULL_TTL_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
        }

        Category category = goods.getCategoryId() == null ? null : categoryMapper.selectById(goods.getCategoryId());
        SeckillSnapshot snapshot = loadNearestAvailableSeckill(goodsId);

        GoodsDetailResponse response = new GoodsDetailResponse();
        response.setGoodsId(goods.getId());
        response.setGoodsName(goods.getName());
        response.setGoodsImg(goods.getCoverImage());
        response.setGoodsImages(parseImages(goods.getImages()));
        response.setCategoryId(goods.getCategoryId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setDescription(goods.getDescription());
        response.setDetail(goods.getDetail());
        response.setPrice(goods.getPrice());
        response.setStock(goods.getStock());
        response.setSales(goods.getSales());
        response.setStatus(goods.getStatus());
        response.setCreateTime(goods.getCreateTime());
        response.setUpdateTime(goods.getUpdateTime());
        if (snapshot != null) {
            // 商品详情会挂载“最近且仍可参与”的秒杀活动，前台可直接据此展示倒计时和秒杀价。
            response.setIsSeckill(1);
            response.setSeckillPrice(snapshot.seckillGoods().getSeckillPrice());
            response.setSeckillActivityId(snapshot.activity().getId());
            response.setSeckillStartTime(snapshot.activity().getStartTime());
            response.setSeckillEndTime(snapshot.activity().getEndTime());
        } else {
            response.setIsSeckill(0);
        }

        redisUtils.set(GOODS_DETAIL_KEY_PREFIX + goodsId, response, randomGoodsDetailTtl(), TimeUnit.SECONDS);
        return response;
    }

    @Override
    public PageResult<AdminGoodsListResponse> getAdminGoodsList(Long categoryId, String keyword, Integer status, Long current, Long size) {
        long pageNo = normalizeCurrent(current);
        long pageSize = normalizeSize(size);
        List<Long> categoryIds = resolveCategoryIds(categoryId);

        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (!categoryIds.isEmpty()) {
            wrapper.in(Goods::getCategoryId, categoryIds);
        }
        if (status != null) {
            wrapper.eq(Goods::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Goods::getName, keyword.trim())
                    .or()
                    .like(Goods::getDescription, keyword.trim()));
        }
        wrapper.orderByDesc(Goods::getCreateTime, Goods::getId);

        Page<Goods> page = goodsMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResult.empty();
        }

        Map<Long, Category> categoryMap = loadCategoryMap(page.getRecords().stream()
                .map(Goods::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<AdminGoodsListResponse> records = page.getRecords().stream()
                .map(goods -> toAdminListResponse(goods, categoryMap.get(goods.getCategoryId())))
                .toList();
        return PageResult.build(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGoods(AdminGoodsSaveRequest request) {
        Category category = validateLeafCategory(request.getCategoryId());

        Goods goods = new Goods();
        applyGoodsRequest(goods, request);
        if (request.getStatus() == null) {
            // 管理端新增商品默认下架，避免未审核/未完善信息的商品直接暴露到前台。
            goods.setStatus(0);
        }
        goodsMapper.insert(goods);

        clearGoodsCaches(goods.getId(), category.getId());
        return goods.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(Long goodsId, AdminGoodsSaveRequest request) {
        Goods existing = requireGoods(goodsId);
        validateLeafCategory(request.getCategoryId());

        applyGoodsRequest(existing, request);
        goodsMapper.updateById(existing);
        clearGoodsCaches(goodsId, existing.getCategoryId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoodsStatus(Long goodsId, Integer status) {
        Goods goods = requireGoods(goodsId);
        if (status != null && status == 0 && hasOngoingActivity(goodsId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "正在参与秒杀活动的商品不允许下架");
        }

        goods.setStatus(status);
        goodsMapper.updateById(goods);
        clearGoodsCaches(goodsId, goods.getCategoryId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(Long goodsId) {
        Goods goods = requireGoods(goodsId);
        if (hasOngoingActivity(goodsId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "该商品正在参与秒杀活动，无法删除");
        }

        goodsMapper.deleteById(goodsId);
        clearGoodsCaches(goodsId, goods.getCategoryId());
    }

    private void applyGoodsRequest(Goods goods, AdminGoodsSaveRequest request) {
        goods.setName(request.getGoodsName());
        goods.setCategoryId(request.getCategoryId());
        goods.setDescription(request.getDescription());
        goods.setDetail(request.getDetail());
        goods.setPrice(request.getPrice());
        goods.setStock(request.getStock());
        goods.setCoverImage(request.getCoverImage());
        goods.setImages(JsonUtils.toJsonString(request.getGoodsImages() == null ? Collections.emptyList() : request.getGoodsImages()));
        goods.setStatus(request.getStatus() == null ? goods.getStatus() : request.getStatus());
        if (goods.getSales() == null) {
            goods.setSales(0);
        }
    }

    private Goods requireGoods(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
        }
        return goods;
    }

    private Category validateLeafCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
        // 商品只允许挂在二级分类下，保证前台列表和后台统计口径稳定。
        if (category.getParentId() == null || category.getParentId() == 0L) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "商品必须挂在二级分类下");
        }
        return category;
    }

    private List<Long> resolveCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }

        if (category.getParentId() == null || category.getParentId() == 0L) {
            List<Category> children = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                    .select(Category::getId)
                    .eq(Category::getParentId, categoryId));
            Set<Long> ids = new LinkedHashSet<>();
            ids.add(categoryId);
            for (Category child : children) {
                ids.add(child.getId());
            }
            return new ArrayList<>(ids);
        }
        return Collections.singletonList(categoryId);
    }

    private void applyGoodsSort(LambdaQueryWrapper<Goods> wrapper, String sort) {
        if (!StringUtils.hasText(sort) || "default".equals(sort)) {
            wrapper.orderByDesc(Goods::getCreateTime, Goods::getId);
            return;
        }

        switch (sort) {
            case "priceAsc" -> wrapper.orderByAsc(Goods::getPrice, Goods::getId);
            case "priceDesc" -> wrapper.orderByDesc(Goods::getPrice, Goods::getId);
            case "salesDesc" -> wrapper.orderByDesc(Goods::getSales, Goods::getId);
            case "createTimeDesc" -> wrapper.orderByDesc(Goods::getCreateTime, Goods::getId);
            default -> wrapper.orderByDesc(Goods::getCreateTime, Goods::getId);
        }
    }

    private Map<Long, Category> loadCategoryMap(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Map<Long, SeckillGoods> loadOngoingSeckillGoodsMap(Set<Long> goodsIds) {
        if (goodsIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime now = LocalDateTime.now();
        // 商品列表只需要展示“正在秒杀”的标记，因此这里只查当前窗口内的活动，避免列表页挂载过多无效信息。
        List<SeckillActivity> activities = seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .select(SeckillActivity::getId)
                .ne(SeckillActivity::getStatus, 3)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now));
        if (activities.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> activityIds = activities.stream().map(SeckillActivity::getId).toList();
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .in(SeckillGoods::getActivityId, activityIds)
                .in(SeckillGoods::getGoodsId, goodsIds));

        Map<Long, SeckillGoods> result = new HashMap<>();
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            result.putIfAbsent(seckillGoods.getGoodsId(), seckillGoods);
        }
        return result;
    }

    private SeckillSnapshot loadNearestAvailableSeckill(Long goodsId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getGoodsId, goodsId)
                .orderByAsc(SeckillGoods::getActivityId));
        if (seckillGoodsList.isEmpty()) {
            return null;
        }

        List<Long> activityIds = seckillGoodsList.stream().map(SeckillGoods::getActivityId).distinct().toList();
        Map<Long, SeckillActivity> activityMap = seckillActivityMapper.selectBatchIds(activityIds).stream()
                .filter(activity -> activity.getStatus() == null || activity.getStatus() != 3)
                .collect(Collectors.toMap(SeckillActivity::getId, Function.identity()));

        SeckillSnapshot candidate = null;
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            SeckillActivity activity = activityMap.get(seckillGoods.getActivityId());
            if (activity == null || activity.getEndTime() == null || activity.getEndTime().isBefore(now)) {
                continue;
            }
            // 详情页优先挂载离当前时间最近、且还未结束的活动，兼顾进行中和即将开始的场景。
            if (candidate == null || activity.getStartTime().isBefore(candidate.activity().getStartTime())) {
                candidate = new SeckillSnapshot(activity, seckillGoods);
            }
        }
        return candidate;
    }

    private GoodsListResponse toPublicListResponse(Goods goods, Category category, SeckillGoods seckillGoods) {
        GoodsListResponse response = new GoodsListResponse();
        response.setGoodsId(goods.getId());
        response.setGoodsName(goods.getName());
        response.setCoverImage(goods.getCoverImage());
        response.setCategoryId(goods.getCategoryId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setPrice(goods.getPrice());
        response.setStock(goods.getStock());
        response.setSales(goods.getSales());
        response.setStatus(goods.getStatus());
        response.setCreateTime(goods.getCreateTime());
        if (seckillGoods != null) {
            response.setIsSeckill(1);
            response.setSeckillPrice(seckillGoods.getSeckillPrice());
        } else {
            response.setIsSeckill(0);
        }
        return response;
    }

    private AdminGoodsListResponse toAdminListResponse(Goods goods, Category category) {
        AdminGoodsListResponse response = new AdminGoodsListResponse();
        response.setGoodsId(goods.getId());
        response.setGoodsName(goods.getName());
        response.setGoodsImg(goods.getCoverImage());
        response.setCategoryId(goods.getCategoryId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setPrice(goods.getPrice());
        response.setStock(goods.getStock());
        response.setSales(goods.getSales());
        response.setStatus(goods.getStatus());
        response.setStatusDesc(goods.getStatus() != null && goods.getStatus() == 1 ? "上架" : "下架");
        response.setCreateTime(goods.getCreateTime());
        response.setUpdateTime(goods.getUpdateTime());
        return response;
    }

    private List<String> parseImages(String images) {
        List<String> list = JsonUtils.parseList(images, String.class);
        return list == null ? Collections.emptyList() : list;
    }

    private boolean hasOngoingActivity(Long goodsId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getGoodsId, goodsId));
        if (seckillGoodsList.isEmpty()) {
            return false;
        }
        // 下架/删除商品前必须校验是否参与进行中的活动，避免活动页和商品状态出现不一致。
        List<Long> activityIds = seckillGoodsList.stream().map(SeckillGoods::getActivityId).distinct().toList();
        Long count = seckillActivityMapper.selectCount(new LambdaQueryWrapper<SeckillActivity>()
                .in(SeckillActivity::getId, activityIds)
                .ne(SeckillActivity::getStatus, 3)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now));
        return count != null && count > 0;
    }

    private void clearGoodsCaches(Long goodsId, Long categoryId) {
        List<String> keysToDelete = new java.util.ArrayList<>();
        keysToDelete.add(GOODS_DETAIL_KEY_PREFIX + goodsId);
        keysToDelete.add(GOODS_NULL_KEY_PREFIX + goodsId);
        redisUtils.pipelineDelete(keysToDelete);
        redisUtils.deleteByPattern(GOODS_LIST_KEY_PATTERN);
        redisUtils.deleteByPattern(SECKILL_ACTIVITY_LIST_KEY_PATTERN);

        // 商品一旦被修改，关联活动详情中的商品价格/库存文案也可能失效，因此顺手清掉活动详情缓存。
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getGoodsId, goodsId));
        if (!seckillGoodsList.isEmpty()) {
            List<String> activityKeys = seckillGoodsList.stream()
                    .map(sg -> SECKILL_ACTIVITY_KEY_PREFIX + sg.getActivityId())
                    .distinct()
                    .toList();
            redisUtils.pipelineDelete(activityKeys);
        }

        if (categoryId != null) {
            // 分类树缓存粒度较粗，商品分类变更后直接整棵树失效，保证前台分类数据一致。
            redisUtils.delete("category:tree");
        }
    }

    private String buildGoodsListCacheKey(Long categoryId, String keyword, String sort, long current, long size) {
        return GOODS_LIST_KEY_PREFIX
                + (categoryId == null ? "all" : categoryId) + ":"
                + (StringUtils.hasText(keyword) ? keyword.trim() : "all") + ":"
                + (StringUtils.hasText(sort) ? sort : "default") + ":"
                + current + ":" + size;
    }

    private long randomGoodsListTtl() {
        return GOODS_LIST_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(GOODS_RANDOM_TTL_SECONDS + 1);
    }

    private long randomGoodsDetailTtl() {
        return GOODS_DETAIL_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(GOODS_RANDOM_TTL_SECONDS + 1);
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private record SeckillSnapshot(SeckillActivity activity, SeckillGoods seckillGoods) {
    }
}
