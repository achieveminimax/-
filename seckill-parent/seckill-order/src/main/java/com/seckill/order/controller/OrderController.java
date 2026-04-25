package com.seckill.order.controller;

import com.seckill.common.dto.PageRequest;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.order.dto.OrderDetailResponse;
import com.seckill.order.dto.OrderListResponse;
import com.seckill.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单相关接口")
@Validated
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 订单列表查询
     */
    @Operation(summary = "订单列表", description = "查询当前用户的订单列表，支持按状态筛选")
    @GetMapping("/list")
    public Result<PageResult<OrderListResponse>> list(
            @Parameter(description = "订单状态：1-待支付 2-已支付 3-已发货 4-已完成 5-已取消") @RequestParam(required = false) Integer status,
            @Valid PageRequest pageRequest) {
        PageResult<OrderListResponse> result = orderService.listUserOrders(
                status,
                pageRequest.getCurrent(),
                pageRequest.getSize()
        );
        return Result.success(result);
    }

    /**
     * 订单详情查询
     */
    @Operation(summary = "订单详情", description = "查询指定订单的详细信息")
    @GetMapping("/{orderNo}")
    public Result<OrderDetailResponse> detail(
            @Parameter(description = "订单编号", required = true) @NotBlank(message = "订单编号不能为空")
            @PathVariable String orderNo) {
        OrderDetailResponse detail = orderService.getOrderDetail(orderNo);
        return Result.success(detail);
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单", description = "取消待支付的订单")
    @PutMapping("/cancel/{orderNo}")
    public Result<Void> cancel(
            @Parameter(description = "订单编号", required = true) @NotBlank(message = "订单编号不能为空")
            @PathVariable String orderNo,
            @Parameter(description = "取消原因") @RequestParam(required = false) String cancelReason) {
        orderService.cancelOrder(orderNo, cancelReason);
        return Result.success();
    }
}
