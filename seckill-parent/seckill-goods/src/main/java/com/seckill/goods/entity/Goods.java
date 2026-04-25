package com.seckill.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品实体。
 * <p>
 * 继承 {@link BaseEntity} 后，会自动带上主键、创建时间、更新时间和逻辑删除字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_goods")
public class Goods extends BaseEntity {
    /** 商品名称。 */
    private String name;
    /** 商品简介。 */
    private String description;
    /** 商品原价。 */
    private BigDecimal price;
    /** 普通库存。 */
    private Integer stock;

    /** 所属分类 ID，要求挂在二级分类下。 */
    @TableField("category_id")
    private Long categoryId;

    /** 商品封面图。 */
    @TableField("cover_image")
    private String coverImage;

    /** 商品轮播图，使用 JSON 数组字符串存储。 */
    private String images;
    /** 商品详情富文本。 */
    private String detail;
    /** 销量。 */
    private Integer sales;
    /** 商品状态：0-下架，1-上架。 */
    private Integer status;
}
