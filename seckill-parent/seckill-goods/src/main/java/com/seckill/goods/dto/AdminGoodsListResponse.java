package com.seckill.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商品列表响应体。
 */
@Data
public class AdminGoodsListResponse {
    /** 商品 ID。 */
    private Long goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 商品主图。 */
    private String goodsImg;
    /** 分类 ID。 */
    private Long categoryId;
    /** 分类名称。 */
    private String categoryName;
    /** 商品价格。 */
    private BigDecimal price;
    /** 库存。 */
    private Integer stock;
    /** 销量。 */
    private Integer sales;
    /** 状态码。 */
    private Integer status;
    /** 状态文案。 */
    private String statusDesc;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
