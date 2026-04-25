package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.dto.seckill.AdminSeckillStatisticsResponse;
import com.seckill.admin.dto.stats.DashboardStatsResponse;
import com.seckill.admin.dto.stats.SalesStatsResponse;
import com.seckill.admin.dto.stats.TopProductsResponse;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.admin.service.AdminSeckillService;
import com.seckill.admin.service.AdminStatisticsService;
import com.seckill.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端数据统计入口。
 * <p>
 * 提供仪表盘、销售统计、秒杀统计等数据接口，返回 ECharts 格式数据。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;
    private final AdminSeckillService adminSeckillService;

    /**
     * 获取仪表盘汇总数据。
     *
     * @return 仪表盘统计数据
     */
    @GetMapping("/dashboard")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<DashboardStatsResponse> dashboard() {
        return Result.success(adminStatisticsService.getDashboardStats());
    }

    /**
     * 获取销售统计（ECharts 格式）。
     *
     * @param days 统计天数，默认 7 天
     * @return 销售统计数据
     */
    @GetMapping("/stats/sales")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<SalesStatsResponse> salesStats(
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        return Result.success(adminStatisticsService.getSalesStats(days));
    }

    /**
     * 获取秒杀活动统计。
     *
     * @param activityId 活动 ID
     * @return 秒杀活动统计数据
     */
    @GetMapping("/stats/seckill/{activityId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<AdminSeckillStatisticsResponse> seckillStats(@PathVariable Long activityId) {
        return Result.success(adminSeckillService.getActivityStatistics(activityId));
    }

    /**
     * 获取热销商品排行（ECharts 格式）。
     *
     * @return 热销商品排行数据
     */
    @GetMapping("/stats/top-products")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<TopProductsResponse> topProducts() {
        return Result.success(adminStatisticsService.getTopProducts());
    }
}
