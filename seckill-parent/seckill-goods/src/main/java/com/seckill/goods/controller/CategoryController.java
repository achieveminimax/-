package com.seckill.goods.controller;

import com.seckill.common.result.Result;
import com.seckill.goods.dto.CategoryTreeResponse;
import com.seckill.goods.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台商品分类查询入口。
 * <p>
 * 该控制器只提供分类树的只读查询，分类新增、修改、删除统一由管理端控制器负责。
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    /**
     * 分类领域服务。
     */
    private final CategoryService categoryService;

    /**
     * 查询分类树。
     * <p>
     * 返回两级树结构，前端可直接渲染分类导航或分类筛选面板。
     */
    @GetMapping("/list")
    public Result<List<CategoryTreeResponse>> list() {
        return Result.success(categoryService.getCategoryTree());
    }
}
