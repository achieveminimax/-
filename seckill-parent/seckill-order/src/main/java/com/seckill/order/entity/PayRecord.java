package com.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 */
@Data
@TableName("t_pay_record")
public class PayRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付流水号
     */
    @TableField("pay_no")
    private String payNo;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 支付方式：1-余额 2-模拟支付宝 3-模拟微信
     */
    @TableField("pay_method")
    private Integer payMethod;

    /**
     * 支付金额
     */
    @TableField("pay_amount")
    private BigDecimal payAmount;

    /**
     * 支付状态：0-待支付 1-已支付 2-支付失败 3-已退款
     */
    private Integer status;

    /**
     * 第三方交易号
     */
    @TableField("trade_no")
    private String tradeNo;

    /**
     * 支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
