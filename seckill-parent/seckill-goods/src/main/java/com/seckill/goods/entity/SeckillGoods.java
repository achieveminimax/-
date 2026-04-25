package com.seckill.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品关联实体。
 * <p>
 * 用于描述“某个活动下的某个商品”这一层关系，保存活动期价格、库存和限购信息。
 */
@Data
@TableName("t_seckill_goods")
public class SeckillGoods {
    /** 关联记录主键。 */
    @TableId
    private Long id;

    /** 所属活动 ID。 */
    @TableField("activity_id")
    private Long activityId;

    /** 对应普通商品 ID。 */
    @TableField("goods_id")
    private Long goodsId;

    /** 秒杀价。 */
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    /** 秒杀库存。 */
    @TableField("seckill_stock")
    private Integer seckillStock;

    /** 单用户限购数。 */
    @TableField("limit_per_user")
    private Integer limitPerUser;

    /** 已售数量。 */
    @TableField("sales_count")
    private Integer salesCount;

    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
