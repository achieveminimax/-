package com.seckill.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 前台商品列表响应体。
 */
@Data
public class GoodsListResponse {
    /** 商品 ID。 */
    private Long goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 封面图。 */
    private String coverImage;
    /** 分类 ID。 */
    private Long categoryId;
    /** 分类名称。 */
    private String categoryName;
    /** 商品原价。 */
    private BigDecimal price;
    /** 秒杀价，未参与秒杀时为 null。 */
    private BigDecimal seckillPrice;
    /** 普通库存。 */
    private Integer stock;
    /** 销量。 */
    private Integer sales;
    /** 商品状态。 */
    private Integer status;
    /** 是否参与秒杀：0-否，1-是。 */
    private Integer isSeckill;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
