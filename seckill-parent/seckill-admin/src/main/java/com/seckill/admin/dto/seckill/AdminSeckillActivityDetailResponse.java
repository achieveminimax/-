package com.seckill.admin.dto.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端秒杀活动详情响应。
 */
@Data
public class AdminSeckillActivityDetailResponse {

    private Long activityId;
    private String activityName;
    private String description;
    private String activityImg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private List<String> rules;
    private List<AdminSeckillGoodsDetailResponse> goodsList;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 活动内商品详情。
     */
    @Data
    public static class AdminSeckillGoodsDetailResponse {
        private Long goodsId;
        private String goodsName;
        private String goodsImg;
        private BigDecimal originalPrice;
        private BigDecimal seckillPrice;
        private Integer seckillStock;
        private Integer salesCount;
        private Integer limitPerUser;
    }
}
