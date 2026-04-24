package com.seckill.user.controller;

import com.seckill.common.result.Result;
import com.seckill.common.utils.UserContext;
import com.seckill.user.dto.*;
import com.seckill.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserRegisterResponse response = userService.register(request);
        return Result.created(response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        // 从请求头中提取 Token
        String token = extractToken(authorization);
        Long userId = UserContext.getCurrentUserId();
        userService.logout(token, userId);
        return Result.success();
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh-token")
    public Result<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = userService.refreshToken(request);
        return Result.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        UserInfoResponse response = userService.getUserInfo(userId);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.updateUserInfo(userId, request);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        userService.updatePassword(userId, request);
        return Result.success();
    }

    /**
     * 从 Authorization 头中提取 Token
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
