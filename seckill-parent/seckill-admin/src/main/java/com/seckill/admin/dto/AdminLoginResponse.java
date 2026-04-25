package com.seckill.admin.dto;

import lombok.Data;

/**
 * 管理员登录响应体。
 */
@Data
public class AdminLoginResponse {
    /** 管理员 ID。 */
    private Long adminId;
    /** 登录账号。 */
    private String username;
    /** 真实姓名。 */
    private String realName;
    /** 角色标识。 */
    private String role;
    /** Access Token。 */
    private String token;
    /** Refresh Token。 */
    private String refreshToken;
    /** Access Token 过期秒数。 */
    private Long expiresIn;
}
