package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.goods.dto.AdminGoodsListResponse;
import com.seckill.goods.dto.AdminGoodsSaveRequest;
import com.seckill.goods.dto.GoodsStatusUpdateRequest;
import com.seckill.goods.service.GoodsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端商品维护入口。
 * <p>
 * 提供后台常用的商品列表、创建、修改、上下架和删除能力。
 */
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {

    /**
     * 商品领域服务。
     */
    private final GoodsService goodsService;

    /**
     * 查询管理端商品列表。
     */
    @GetMapping
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<PageResult<AdminGoodsListResponse>> list(@RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(required = false, defaultValue = "1") Long current,
                                                           @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.success(goodsService.getAdminGoodsList(categoryId, keyword, status, current, size));
    }

    /**
     * 新增商品。
     */
    @PostMapping
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Map<String, Long>> create(@Valid @RequestBody AdminGoodsSaveRequest request) {
        Long goodsId = goodsService.createGoods(request);
        return Result.created(Map.of("goodsId", goodsId));
    }

    /**
     * 修改商品。
     */
    @PutMapping("/{id}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> update(@PathVariable("id") Long goodsId, @Valid @RequestBody AdminGoodsSaveRequest request) {
        goodsService.updateGoods(goodsId, request);
        return Result.success();
    }

    /**
     * 更新商品上下架状态。
     */
    @PutMapping("/{id}/status")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> updateStatus(@PathVariable("id") Long goodsId,
                                     @Valid @RequestBody GoodsStatusUpdateRequest request) {
        goodsService.updateGoodsStatus(goodsId, request.getStatus());
        return Result.success();
    }

    /**
     * 删除商品。
     */
    @DeleteMapping("/{id}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN})
    public Result<Void> delete(@PathVariable("id") Long goodsId) {
        goodsService.deleteGoods(goodsId);
        return Result.success();
    }
}
