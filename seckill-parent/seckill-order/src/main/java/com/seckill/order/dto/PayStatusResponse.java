package com.seckill.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付状态查询响应
 */
@Data
public class PayStatusResponse {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 支付流水号
     */
    private String payNo;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 支付方式描述
     */
    private String payTypeDesc;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 支付状态描述
     */
    private String statusDesc;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;
}
