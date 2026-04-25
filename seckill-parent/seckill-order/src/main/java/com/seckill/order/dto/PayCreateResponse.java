package com.seckill.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建支付响应
 */
@Data
public class PayCreateResponse {

    /**
     * 支付流水号
     */
    private String payNo;

    /**
     * 订单编号
     */
    private String orderNo;

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
     * 支付参数（模拟）
     */
    private PayParams payParams;

    /**
     * 支付过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付参数内部类
     */
    @Data
    public static class PayParams {
        /**
         * 模拟支付跳转URL
         */
        private String payUrl;

        /**
         * 模拟支付二维码
         */
        private String qrCode;
    }
}
