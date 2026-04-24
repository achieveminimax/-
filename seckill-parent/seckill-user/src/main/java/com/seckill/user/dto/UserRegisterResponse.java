package com.seckill.user.dto;

import lombok.Data;

/**
 * 用户注册响应 DTO
 */
@Data
public class UserRegisterResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

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
