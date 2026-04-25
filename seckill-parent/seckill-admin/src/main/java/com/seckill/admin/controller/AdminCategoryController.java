package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.result.Result;
import com.seckill.goods.dto.CategoryRequest;
import com.seckill.goods.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端分类维护入口。
 */
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    /**
     * 分类领域服务。
     */
    private final CategoryService categoryService;

    /**
     * 新增分类。
     */
    @PostMapping
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Map<String, Long>> create(@Valid @RequestBody CategoryRequest request) {
        Long categoryId = categoryService.createCategory(request);
        return Result.created(Map.of("categoryId", categoryId));
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{id}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> update(@PathVariable("id") Long categoryId, @Valid @RequestBody CategoryRequest request) {
        categoryService.updateCategory(categoryId, request);
        return Result.success();
    }

    /**
     * 删除分类。
     */
    @DeleteMapping("/{id}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN})
    public Result<Void> delete(@PathVariable("id") Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.success();
    }
}
