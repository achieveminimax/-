package com.seckill.admin.dto.stats;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端仪表盘统计数据响应。
 */
@Data
public class DashboardStatsResponse {

    private OverviewStats overview;
    private List<SalesTrendItem> salesTrend;
    private List<TopSellingProduct> topSellingGoods;
    private List<RecentOrder> recentOrders;

    /**
     * 概览统计。
     */
    @Data
    public static class OverviewStats {
        private Long totalUsers;
        private Long todayNewUsers;
        private Long totalOrders;
        private Long todayOrders;
        private BigDecimal totalSales;
        private BigDecimal todaySales;
        private Long totalGoods;
        private Long onlineGoods;
        private Long totalSeckillActivities;
        private Long ongoingActivities;
    }

    /**
     * 销售趋势项。
     */
    @Data
    public static class SalesTrendItem {
        private String date;
        private BigDecimal amount;
        private Long orderCount;
    }

    /**
     * 热销商品。
     */
    @Data
    public static class TopSellingProduct {
        private Long goodsId;
        private String goodsName;
        private Long sales;
        private BigDecimal amount;
    }

    /**
     * 最近订单。
     */
    @Data
    public static class RecentOrder {
        private String orderNo;
        private String username;
        private String goodsName;
        private BigDecimal totalAmount;
        private Integer status;
        private String statusDesc;
        private String createTime;
    }
}
