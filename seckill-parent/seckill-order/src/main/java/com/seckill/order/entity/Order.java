package com.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("activity_id")
    private Long activityId;

    @TableField("goods_name")
    private String goodsName;

    @TableField("goods_image")
    private String goodsImage;

    @TableField("order_price")
    private BigDecimal orderPrice;

    private Integer quantity;
    private Integer status;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receiver_phone")
    private String receiverPhone;

    @TableField("receiver_address")
    private String receiverAddress;

    @TableField("address_id")
    private Long addressId;

    @TableField("pay_type")
    private Integer payType;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("express_company")
    private String expressCompany;

    @TableField("express_no")
    private String expressNo;

    @TableField("ship_time")
    private LocalDateTime shipTime;

    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private Integer deleted;
}
