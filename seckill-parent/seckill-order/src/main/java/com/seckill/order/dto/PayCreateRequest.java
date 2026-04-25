package com.seckill.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建支付请求
 */
@Data
public class PayCreateRequest {

    /**
     * 订单编号
     */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 支付方式：1-余额 2-模拟支付宝 3-模拟微信
     */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
