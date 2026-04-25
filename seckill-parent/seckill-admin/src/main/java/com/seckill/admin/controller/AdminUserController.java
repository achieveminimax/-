package com.seckill.admin.controller;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.dto.user.AdminUserDetailResponse;
import com.seckill.admin.dto.user.AdminUserListResponse;
import com.seckill.admin.dto.user.AdminUserStatusUpdateRequest;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.admin.mapper.AdminUserQueryMapper;
import com.seckill.common.result.PageResult;
import com.seckill.common.result.Result;
import com.seckill.common.utils.AdminContext;
import com.seckill.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端用户管理入口。
 * <p>
 * 提供用户列表查询、详情查看、禁用/启用等后台管理功能。
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    /**
     * 管理端用户查询 Mapper。
     */
    private final AdminUserQueryMapper adminUserQueryMapper;

    /**
     * 管理端用户服务。
     */
    private final UserAdminService userAdminService;

    /**
     * 查询用户列表。
     *
     * @param keyword 搜索关键词（用户名/昵称/手机号）
     * @param status  用户状态：0-禁用、1-正常
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页用户列表
     */
    @GetMapping("/list")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<PageResult<AdminUserListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Long current,
            @RequestParam(required = false, defaultValue = "10") Long size) {

        long offset = (current - 1) * size;
        long total = adminUserQueryMapper.count(keyword, status);
        List<AdminUserListResponse> records = adminUserQueryMapper.selectPage(keyword, status, offset, size);

        PageResult<AdminUserListResponse> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(size);
        result.setCurrent(current);
        result.setPages((total + size - 1) / size);

        return Result.success(result);
    }

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN, AdminRoleEnum.OPERATOR})
    public Result<AdminUserDetailResponse> detail(@PathVariable Long userId) {
        AdminUserDetailResponse detail = adminUserQueryMapper.selectDetail(userId);
        return Result.success(detail);
    }

    /**
     * 更新用户状态（禁用/启用）。
     *
     * @param userId  用户 ID
     * @param request 状态更新请求
     * @return 操作结果
     */
    @PutMapping("/status/{userId}")
    @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.ADMIN})
    public Result<Void> updateStatus(@PathVariable Long userId,
                                     @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        // 禁止操作自己
        Long currentAdminId = AdminContext.getCurrentAdminId();
        if (userId.equals(currentAdminId)) {
            return Result.error(403, "不能操作自己的账号");
        }

        userAdminService.updateUserStatus(userId, request.getStatus());
        return Result.success();
    }
}
