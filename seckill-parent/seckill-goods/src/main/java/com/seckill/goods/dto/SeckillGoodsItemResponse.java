package com.seckill.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀活动中的商品项响应体。
 */
@Data
public class SeckillGoodsItemResponse {
    /** 商品 ID。 */
    private Long goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 商品主图。 */
    private String goodsImg;
    /** 商品原价。 */
    private BigDecimal originalPrice;
    /** 秒杀价。 */
    private BigDecimal seckillPrice;
    /** 当前对外展示库存。 */
    private Integer stock;
    /** 活动配置总库存。 */
    private Integer totalStock;
    /** 实时剩余库存。 */
    private Integer remainStock;
    /** 单用户限购数。 */
    private Integer limitPerUser;
    /** 已售数量。 */
    private Integer salesCount;
}
