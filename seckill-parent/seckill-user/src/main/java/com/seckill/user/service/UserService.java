package com.seckill.user.service;

import com.seckill.user.dto.*;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册响应
     */
    UserRegisterResponse register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 用户登出
     *
     * @param token   当前Token
     * @param userId  用户ID
     */
    void logout(String token, Long userId);

    /**
     * 刷新Token
     *
     * @param request 刷新请求
     * @return 刷新响应
     */
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 更新请求
     */
    void updateUserInfo(Long userId, UserUpdateRequest request);

    /**
     * 更新密码
     *
     * @param userId  用户ID
     * @param request 密码更新请求
     */
    void updatePassword(Long userId, PasswordUpdateRequest request);
}
