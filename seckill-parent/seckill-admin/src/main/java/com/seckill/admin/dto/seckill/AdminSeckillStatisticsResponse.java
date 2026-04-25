package com.seckill.admin.dto.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端秒杀活动统计响应。
 */
@Data
public class AdminSeckillStatisticsResponse {

    private Long activityId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalStock;
    private Integer totalSales;
    private Double salesRate;
    private BigDecimal totalAmount;
    private Long totalUsers;
    private Long totalRequests;
    private Double successRate;
    private Long averageResponseTime;
    private List<GoodsStatistics> goodsStatistics;
    private List<TimeDistribution> timeDistribution;

    /**
     * 商品统计。
     */
    @Data
    public static class GoodsStatistics {
        private Long goodsId;
        private String goodsName;
        private BigDecimal seckillPrice;
        private Integer totalStock;
        private Integer totalSales;
        private Double salesRate;
        private BigDecimal totalAmount;
    }

    /**
     * 时间分布。
     */
    @Data
    public static class TimeDistribution {
        private String time;
        private Integer sales;
    }
}
