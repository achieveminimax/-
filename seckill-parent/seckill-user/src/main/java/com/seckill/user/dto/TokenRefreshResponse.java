package com.seckill.user.dto;

import lombok.Data;

/**
 * Token 刷新响应 DTO
 */
@Data
public class TokenRefreshResponse {

    /**
     * 新的 JWT Token
     */
    private String token;

    /**
     * 新的 Refresh Token
     */
    private String refreshToken;

    /**
     * Token 有效期（秒）
     */
    private Long expiresIn;
}
