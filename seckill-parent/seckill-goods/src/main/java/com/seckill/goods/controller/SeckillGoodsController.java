package com.seckill.goods.controller;

import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.goods.dto.SeckillActivityDetailResponse;
import com.seckill.goods.dto.SeckillActivityListResponse;
import com.seckill.goods.service.SeckillGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台秒杀活动查询入口。
 * <p>
 * M3 阶段只开放“活动列表”和“活动详情”两个只读接口，
 * 真正执行秒杀、秒杀地址校验等高并发写操作放到后续里程碑实现。
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillGoodsController {

    private final SeckillGoodsService seckillGoodsService;

    /**
     * 查询秒杀活动列表。
     * <p>
     * 默认返回“进行中 + 即将开始”的活动，具体排序与库存刷新逻辑都由 service 统一处理，
     * controller 只负责参数透传和响应包装。
     */
    @GetMapping("/list")
    public Result<PageResult<SeckillActivityListResponse>> list(@RequestParam(required = false) Integer status,
                                                                @RequestParam(required = false, defaultValue = "1") Long current,
                                                                @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.success(seckillGoodsService.getActivityList(status, current, size));
    }

    /**
     * 查询单个秒杀活动详情。
     * <p>
     * 返回活动基础信息、规则文案以及活动下商品列表，
     * 商品剩余库存会优先从 Redis 中读取，保证前台看到的是接近实时的数据。
     */
    @GetMapping("/{activityId}")
    public Result<SeckillActivityDetailResponse> detail(@PathVariable Long activityId) {
        return Result.success(seckillGoodsService.getActivityDetail(activityId));
    }
}
