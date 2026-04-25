package com.seckill.order.controller;

import com.seckill.common.result.Result;
import com.seckill.order.dto.PayCallbackRequest;
import com.seckill.order.dto.PayCreateRequest;
import com.seckill.order.dto.PayCreateResponse;
import com.seckill.order.dto.PayStatusResponse;
import com.seckill.order.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 */
@Tag(name = "支付管理", description = "支付相关接口")
@Validated
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 发起支付
     */
    @Operation(summary = "发起支付", description = "对指定订单发起支付")
    @PostMapping("/create")
    public Result<PayCreateResponse> create(
            @Valid @RequestBody PayCreateRequest request) {
        PayCreateResponse response = payService.createPay(request.getOrderNo(), request.getPayType());
        return Result.success(response);
    }

    /**
     * 支付回调（模拟）
     */
    @Operation(summary = "支付回调", description = "模拟第三方支付回调")
    @PostMapping("/callback")
    public Result<Void> callback(
            @Valid @RequestBody PayCallbackRequest request) {
        payService.payCallback(request.getPayNo(), request.getTradeNo());
        return Result.success();
    }

    /**
     * 查询支付状态
     */
    @Operation(summary = "查询支付状态", description = "查询指定订单的支付状态")
    @GetMapping("/status/{orderNo}")
    public Result<PayStatusResponse> status(
            @Parameter(description = "订单编号", required = true)
            @NotBlank(message = "订单编号不能为空")
            @PathVariable String orderNo) {
        PayStatusResponse response = payService.queryPayStatus(orderNo);
        return Result.success(response);
    }

    /**
     * 模拟支付页面（用于测试）
     */
    @Operation(summary = "模拟支付", description = "模拟支付页面，用于测试")
    @GetMapping("/mock/{payNo}")
    public Result<String> mockPay(
            @Parameter(description = "支付流水号", required = true)
            @NotBlank(message = "支付流水号不能为空")
            @PathVariable String payNo) {
        // 模拟支付成功，直接回调
        String mockTradeNo = "MOCK" + System.currentTimeMillis();
        payService.payCallback(payNo, mockTradeNo);
        return Result.success("支付成功");
    }
}
