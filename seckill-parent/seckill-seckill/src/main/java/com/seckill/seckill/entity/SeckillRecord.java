package com.seckill.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_seckill_record")
public class SeckillRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("activity_id")
    private Long activityId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("order_id")
    private Long orderId;

    private Integer status;

    @TableField("fail_reason")
    private String failReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;
}
