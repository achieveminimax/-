package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.result.Result;
import com.seckill.goods.service.SeckillPreheatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端秒杀预热入口。
 * <p>
 * 主要用于运营或开发测试时手动触发预热，验证活动详情和库存是否已提前写入 Redis。
 */
@RestController
@RequestMapping("/api/admin/seckill/preheat")
@RequiredArgsConstructor
public class AdminSeckillPreheatController {

    /**
     * 秒杀预热服务。
     */
    private final SeckillPreheatService seckillPreheatService;

    /**
     * 手动预热单个活动。
     */
    @PostMapping("/{activityId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<Void> preheatActivity(@PathVariable Long activityId) {
        seckillPreheatService.preheatActivity(activityId);
        return Result.success();
    }

    /**
     * 批量预热未来 5 分钟内的活动。
     */
    @PostMapping("/upcoming")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<Map<String, Integer>> preheatUpcomingActivities() {
        int count = seckillPreheatService.preheatUpcomingActivities();
        return Result.success(Map.of("preheatedCount", count));
    }
}
