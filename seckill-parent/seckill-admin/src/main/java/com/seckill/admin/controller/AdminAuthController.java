package com.seckill.admin.controller;

import com.seckill.admin.dto.AdminLoginRequest;
import com.seckill.admin.dto.AdminLoginResponse;
import com.seckill.admin.service.AdminAuthService;
import com.seckill.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证入口。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    /**
     * 管理员认证服务。
     */
    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录。
     * <p>
     * 登录成功后返回 access token、refresh token 和管理员基础信息。
     */
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success(adminAuthService.login(request));
    }
}
