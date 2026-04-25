package com.seckill.admin.service;

import com.seckill.admin.dto.stats.DashboardStatsResponse;
import com.seckill.admin.dto.stats.SalesStatsResponse;
import com.seckill.admin.dto.stats.TopProductsResponse;

/**
 * 管理端统计服务接口。
 */
public interface AdminStatisticsService {

    /**
     * 获取仪表盘统计数据。
     *
     * @return 仪表盘统计数据
     */
    DashboardStatsResponse getDashboardStats();

    /**
     * 获取销售统计（ECharts 格式）。
     *
     * @param days 统计天数
     * @return 销售统计数据
     */
    SalesStatsResponse getSalesStats(Integer days);

    /**
     * 获取热销商品排行（ECharts 格式）。
     *
     * @return 热销商品排行数据
     */
    TopProductsResponse getTopProducts();
}
