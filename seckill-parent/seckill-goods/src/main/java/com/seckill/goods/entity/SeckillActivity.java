package com.seckill.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动实体。
 * <p>
 * 该实体描述活动层面的元信息，例如活动名称、时间窗口、展示图片和活动状态。
 */
@Data
@TableName("t_seckill_activity")
public class SeckillActivity {
    /** 活动主键。 */
    @TableId
    private Long id;

    /** 活动名称。 */
    @TableField("activity_name")
    private String activityName;

    /** 活动描述。 */
    private String description;

    /** 活动展示图。 */
    @TableField("activity_img")
    private String activityImg;

    /** 活动开始时间。 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 活动结束时间。 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 活动状态字段，展示时还会结合当前时间动态计算。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
