package com.seckill.goods.controller;

import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.goods.dto.GoodsDetailResponse;
import com.seckill.goods.dto.GoodsListResponse;
import com.seckill.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台商品查询入口。
 * <p>
 * 对用户开放商品列表和商品详情两个读接口，管理端写操作由后台控制器单独处理。
 */
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    /**
     * 商品领域服务。
     */
    private final GoodsService goodsService;

    /**
     * 查询前台商品列表。
     * <p>
     * 支持分类筛选、关键词搜索、排序和分页，返回结果里会附带当前商品是否参与秒杀的标记。
     */
    @GetMapping("/list")
    public Result<PageResult<GoodsListResponse>> list(@RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false, defaultValue = "default") String sort,
                                                      @RequestParam(required = false, defaultValue = "1") Long current,
                                                      @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.success(goodsService.getPublicGoodsList(categoryId, keyword, sort, current, size));
    }

    /**
     * 查询商品详情。
     * <p>
     * 返回商品基础信息、图片列表、分类信息，以及最近可参与的秒杀活动信息。
     */
    @GetMapping("/{id}")
    public Result<GoodsDetailResponse> detail(@PathVariable("id") Long goodsId) {
        return Result.success(goodsService.getPublicGoodsDetail(goodsId));
    }
}
