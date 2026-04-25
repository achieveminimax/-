package com.seckill.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 前台商品详情响应体。
 * <p>
 * 除普通商品信息外，还会附带最近可参与的秒杀活动信息。
 */
@Data
public class GoodsDetailResponse {
    /** 商品 ID。 */
    private Long goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 商品主图。 */
    private String goodsImg;
    /** 商品多图。 */
    private List<String> goodsImages;
    /** 分类 ID。 */
    private Long categoryId;
    /** 分类名称。 */
    private String categoryName;
    /** 商品简介。 */
    private String description;
    /** 商品详情富文本。 */
    private String detail;
    /** 商品原价。 */
    private BigDecimal price;
    /** 秒杀价。 */
    private BigDecimal seckillPrice;
    /** 普通库存。 */
    private Integer stock;
    /** 销量。 */
    private Integer sales;
    /** 商品状态。 */
    private Integer status;
    /** 是否关联秒杀活动。 */
    private Integer isSeckill;
    /** 秒杀活动 ID。 */
    private Long seckillActivityId;
    /** 秒杀开始时间。 */
    private LocalDateTime seckillStartTime;
    /** 秒杀结束时间。 */
    private LocalDateTime seckillEndTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
