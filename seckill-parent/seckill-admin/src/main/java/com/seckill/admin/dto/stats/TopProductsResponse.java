package com.seckill.admin.dto.stats;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端热销商品排行响应（ECharts 格式）。
 */
@Data
public class TopProductsResponse {

    private List<String> xAxis;
    private List<SeriesItem> series;

    /**
     * 数据系列。
     */
    @Data
    public static class SeriesItem {
        private String name;
        private String type;
        private List<Long> data;
    }
}
