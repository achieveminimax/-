package com.seckill.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录请求体。
 */
@Data
public class AdminLoginRequest {

    /** 管理员账号。 */
    @NotBlank(message = "管理员账号不能为空")
    private String username;

    /** 管理员密码。 */
    @NotBlank(message = "管理员密码不能为空")
    private String password;
}
