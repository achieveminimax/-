package com.seckill.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.dto.CategoryRequest;
import com.seckill.goods.dto.CategoryTreeResponse;
import com.seckill.goods.entity.Category;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.mapper.CategoryMapper;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.service.CategoryService;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分类领域核心实现。
 * <p>
 * 主要负责三件事：
 * <ul>
 *     <li>把数据库中的平铺分类组装为前台可直接使用的树结构</li>
 *     <li>处理管理端的新增、修改、删除操作</li>
 *     <li>在分类发生变更时清理分类树和商品相关缓存</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORY_TREE_KEY = "category:tree";
    private static final String GOODS_LIST_KEY_PATTERN = "goods:list:*";
    private static final String GOODS_DETAIL_KEY_PREFIX = "goods:detail:";
    private static final long CATEGORY_TREE_BASE_TTL_SECONDS = 3600L;
    private static final long CATEGORY_TREE_RANDOM_TTL_SECONDS = 300L;

    private final CategoryMapper categoryMapper;
    private final GoodsMapper goodsMapper;
    private final RedisUtils redisUtils;

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        // 分类树属于高频低变更数据，优先从 Redis 读取。
        List<CategoryTreeResponse> cached = redisUtils.get(CATEGORY_TREE_KEY);
        if (cached != null) {
            return cached;
        }

        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getParentId, Category::getSort, Category::getId));

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        List<CategoryTreeResponse> tree = buildCategoryTree(categories);
        redisUtils.set(CATEGORY_TREE_KEY, tree, randomCategoryTreeTtl(), TimeUnit.SECONDS);
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryRequest request) {
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();
        // 新增前先校验父分类是否合法，保证项目只维护两级分类结构。
        validateParentCategory(parentId);

        Category category = new Category();
        category.setParentId(parentId);
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        categoryMapper.insert(category);

        clearCategoryCaches();
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long categoryId, CategoryRequest request) {
        Category existing = requireCategory(categoryId);
        Long targetParentId = request.getParentId() == null ? existing.getParentId() : request.getParentId();

        if (!categoryId.equals(targetParentId)) {
            validateParentCategory(targetParentId);
        } else {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "分类不能将自己设为父分类");
        }

        existing.setParentId(targetParentId);
        existing.setName(request.getName());
        existing.setIcon(request.getIcon());
        existing.setSort(request.getSort() == null ? existing.getSort() : request.getSort());
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        categoryMapper.updateById(existing);

        // 分类变更会影响分类树和商品详情中的分类信息，因此需要联动清理缓存。
        clearCategoryCaches();
        clearGoodsDetailCachesByCategory(categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long categoryId) {
        Category category = requireCategory(categoryId);

        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "该分类下存在子分类，无法删除");
        }

        Long goodsCount = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getCategoryId, categoryId));
        if (goodsCount != null && goodsCount > 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "该分类下存在商品，无法删除");
        }

        categoryMapper.deleteById(category.getId());
        clearCategoryCaches();
    }

    private List<CategoryTreeResponse> buildCategoryTree(List<Category> categories) {
        Map<Long, CategoryTreeResponse> responseMap = categories.stream()
                .map(this::toTreeResponse)
                .collect(Collectors.toMap(CategoryTreeResponse::getCategoryId, Function.identity()));

        // 先把所有节点放入 map，再按 parentId 串起父子关系，可避免递归重复扫描列表。
        List<CategoryTreeResponse> roots = new ArrayList<>();
        for (Category category : categories) {
            CategoryTreeResponse current = responseMap.get(category.getId());
            if (category.getParentId() == null || category.getParentId() == 0L) {
                roots.add(current);
                continue;
            }
            CategoryTreeResponse parent = responseMap.get(category.getParentId());
            if (parent != null) {
                parent.getChildren().add(current);
            }
        }
        return roots;
    }

    private CategoryTreeResponse toTreeResponse(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse();
        response.setCategoryId(category.getId());
        response.setCategoryName(category.getName());
        response.setIcon(category.getIcon());
        response.setSort(category.getSort());
        response.setParentId(category.getParentId());
        return response;
    }

    private void validateParentCategory(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }

        Category parent = requireCategory(parentId);
        // 当前项目约束只支持两级分类，因此父分类必须是一级分类。
        if (parent.getParentId() != null && parent.getParentId() != 0L) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "仅支持两级分类");
        }
        if (parent.getStatus() != null && parent.getStatus() == 0) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "父分类已禁用");
        }
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private void clearCategoryCaches() {
        redisUtils.delete(CATEGORY_TREE_KEY);
        redisUtils.deleteByPattern(GOODS_LIST_KEY_PATTERN);
    }

    private void clearGoodsDetailCachesByCategory(Long categoryId) {
        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .select(Goods::getId)
                .eq(Goods::getCategoryId, categoryId));
        if (goodsList.isEmpty()) {
            return;
        }
        // 这里不直接删商品列表模式缓存，是因为分类级缓存已在 clearCategoryCaches() 中统一处理。
        List<String> keysToDelete = goodsList.stream()
                .map(Goods::getId)
                .filter(java.util.Objects::nonNull)
                .map(id -> GOODS_DETAIL_KEY_PREFIX + id)
                .toList();
        redisUtils.pipelineDelete(keysToDelete);
    }

    private long randomCategoryTreeTtl() {
        return CATEGORY_TREE_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(CATEGORY_TREE_RANDOM_TTL_SECONDS + 1);
    }
}
