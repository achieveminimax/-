package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.dto.seckill.AdminSeckillActivityDetailResponse;
import com.seckill.admin.dto.seckill.AdminSeckillActivityRequest;
import com.seckill.admin.dto.seckill.AdminSeckillActivityResponse;
import com.seckill.admin.dto.seckill.AdminSeckillStatisticsResponse;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.admin.service.AdminSeckillService;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端秒杀活动管理入口。
 * <p>
 * 提供秒杀活动的创建、修改、查询和统计功能。
 */
@RestController
@RequestMapping("/api/admin/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final AdminSeckillService adminSeckillService;

    /**
     * 查询秒杀活动列表。
     *
     * @param status  活动状态：0-全部、1-未开始、2-进行中、3-已结束
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页活动列表
     */
    @GetMapping("/activity")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<PageResult<AdminSeckillActivityResponse>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Long current,
            @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.success(adminSeckillService.getActivityList(status, current, size));
    }

    /**
     * 查询秒杀活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    @GetMapping("/activity/{activityId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<AdminSeckillActivityDetailResponse> detail(@PathVariable Long activityId) {
        return Result.success(adminSeckillService.getActivityDetail(activityId));
    }

    /**
     * 创建秒杀活动。
     *
     * @param request 活动创建请求
     * @return 创建的活动 ID
     */
    @PostMapping("/activity")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Map<String, Long>> create(@Valid @RequestBody AdminSeckillActivityRequest request) {
        Long activityId = adminSeckillService.createActivity(request);
        return Result.created(Map.of("activityId", activityId));
    }

    /**
     * 修改秒杀活动。
     *
     * @param request 活动修改请求
     * @return 操作结果
     */
    @PutMapping("/activity")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> update(@Valid @RequestBody AdminSeckillActivityRequest request) {
        adminSeckillService.updateActivity(request);
        return Result.success();
    }

    /**
     * 查询秒杀活动统计。
     *
     * @param activityId 活动 ID
     * @return 活动统计数据
     */
    @GetMapping("/statistics/{activityId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<AdminSeckillStatisticsResponse> statistics(@PathVariable Long activityId) {
        return Result.success(adminSeckillService.getActivityStatistics(activityId));
    }
}
