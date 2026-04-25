package com.seckill.admin.dto.stats;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端销售统计响应（ECharts 格式）。
 */
@Data
public class SalesStatsResponse {

    private List<String> xAxis;
    private List<SeriesItem> series;
    private Legend legend;

    /**
     * 数据系列。
     */
    @Data
    public static class SeriesItem {
        private String name;
        private String type;
        private List<BigDecimal> data;
    }

    /**
     * 图例。
     */
    @Data
    public static class Legend {
        private List<String> data;
    }
}
