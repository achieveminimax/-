package com.seckill.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付回调请求
 */
@Data
public class PayCallbackRequest {

    /**
     * 支付流水号
     */
    @NotBlank(message = "支付流水号不能为空")
    private String payNo;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 支付状态：SUCCESS-成功 FAIL-失败
     */
    private String status;
}
