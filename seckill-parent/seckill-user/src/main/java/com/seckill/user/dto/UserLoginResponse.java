package com.seckill.user.dto;

import lombok.Data;

/**
 * 用户登录响应 DTO
 */
@Data
public class UserLoginResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 账号
     */
    private String account;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * JWT Token
     */
    private String token;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * Token 有效期（秒）
     */
    private Long expiresIn;
}
