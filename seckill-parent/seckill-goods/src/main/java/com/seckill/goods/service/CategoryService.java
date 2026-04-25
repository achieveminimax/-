package com.seckill.goods.service;

import com.seckill.goods.dto.CategoryRequest;
import com.seckill.goods.dto.CategoryTreeResponse;

import java.util.List;

/**
 * 商品分类服务接口。
 * <p>
 * 负责分类树查询以及管理端分类维护操作。
 */
public interface CategoryService {

    /**
     * 查询前台分类树。
     *
     * @return 两级树结构分类列表
     */
    List<CategoryTreeResponse> getCategoryTree();

    /**
     * 新增分类。
     *
     * @param request 分类请求参数
     * @return 新增后的分类 ID
     */
    Long createCategory(CategoryRequest request);

    /**
     * 修改分类。
     *
     * @param categoryId 分类 ID
     * @param request 分类请求参数
     */
    void updateCategory(Long categoryId, CategoryRequest request);

    /**
     * 删除分类。
     *
     * @param categoryId 分类 ID
     */
    void deleteCategory(Long categoryId);
}
