package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.dto.order.AdminOrderDetailResponse;
import com.seckill.admin.dto.order.AdminOrderListResponse;
import com.seckill.admin.dto.order.AdminOrderShipRequest;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.admin.mapper.AdminOrderQueryMapper;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.order.service.OrderAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端订单管理入口。
 * <p>
 * 提供订单列表查询、详情查看、发货等后台管理功能。
 */
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    /**
     * 管理端订单查询 Mapper。
     */
    private final AdminOrderQueryMapper adminOrderQueryMapper;

    /**
     * 管理端订单服务。
     */
    private final OrderAdminService orderAdminService;

    /**
     * 查询订单列表。
     *
     * @param orderNo   订单编号（精确匹配）
     * @param userId    用户 ID
     * @param status    订单状态：0-全部、1-待支付、2-已支付、3-已发货、4-已完成、5-已取消、6-已退款
     * @param startTime 下单起始时间
     * @param endTime   下单结束时间
     * @param current   当前页码
     * @param size      每页大小
     * @return 分页订单列表
     */
    @GetMapping("/list")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<PageResult<AdminOrderListResponse>> list(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "1") Long current,
            @RequestParam(required = false, defaultValue = "10") Long size) {

        long offset = (current - 1) * size;
        long total = adminOrderQueryMapper.count(orderNo, userId, status, startTime, endTime);
        List<AdminOrderListResponse> records = adminOrderQueryMapper.selectPage(
                orderNo, userId, status, startTime, endTime, offset, size);

        PageResult<AdminOrderListResponse> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(size);
        result.setCurrent(current);
        result.setPages((total + size - 1) / size);

        return Result.success(result);
    }

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<AdminOrderDetailResponse> detail(@PathVariable String orderNo) {
        AdminOrderDetailResponse detail = adminOrderQueryMapper.selectDetail(orderNo);
        return Result.success(detail);
    }

    /**
     * 订单发货。
     *
     * @param orderNo 订单编号
     * @param request 发货请求
     * @return 操作结果
     */
    @PutMapping("/{orderNo}/ship")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> ship(@PathVariable String orderNo,
                             @Valid @RequestBody AdminOrderShipRequest request) {
        orderAdminService.shipOrder(orderNo, request.getExpressCompany(), request.getExpressNo());
        return Result.success();
    }
}
