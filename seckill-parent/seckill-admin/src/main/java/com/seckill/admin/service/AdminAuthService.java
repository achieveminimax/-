package com.seckill.admin.service;

import com.seckill.admin.dto.AdminLoginRequest;
import com.seckill.admin.dto.AdminLoginResponse;

/**
 * 管理员认证服务接口。
 */
public interface AdminAuthService {

    /**
     * 管理员登录。
     *
     * @param request 登录请求体
     * @return 登录结果，包含 token 和管理员基本信息
     */
    AdminLoginResponse login(AdminLoginRequest request);
}
