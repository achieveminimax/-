package com.seckill.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.admin.dto.AdminLoginRequest;
import com.seckill.admin.dto.AdminLoginResponse;
import com.seckill.admin.entity.Admin;
import com.seckill.admin.mapper.AdminMapper;
import com.seckill.admin.service.AdminAuthService;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.JwtUtils;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 管理端认证服务。
 * <p>
 * 当前阶段只实现最基础的账号密码登录，但已经把 access token / refresh token 的存储方式预留出来，
 * 方便后续继续扩展管理员刷新、退出登录和更细粒度的权限控制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String ADMIN_TOKEN_KEY = "admin:token:";
    private static final String ADMIN_REFRESH_KEY = "admin:refresh:";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 30 * 60L;
    private static final long REFRESH_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

    private final AdminMapper adminMapper;
    private final RedisUtils redisUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 管理员账号是后台固定入口，这里按用户名精确查询，避免和用户侧多账号登录策略混用。
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "管理员账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "管理员账号已被禁用");
        }

        String token = JwtUtils.generateAdminAccessToken(admin.getId(), admin.getRole());
        String refreshToken = JwtUtils.generateAdminRefreshToken(admin.getId());
        // Redis 中只保留当前最后一次登录的 token，拦截器校验时据此判断管理员会话是否有效。
        redisUtils.set(ADMIN_TOKEN_KEY + admin.getId(), token, ACCESS_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        redisUtils.set(ADMIN_REFRESH_KEY + admin.getId(), refreshToken, REFRESH_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 最后登录时间既用于审计，也方便后续做登录态分析和安全排查。
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setAdminId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setRealName(admin.getRealName());
        response.setRole(admin.getRole());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(ACCESS_TOKEN_EXPIRE_SECONDS);

        log.info("管理员登录成功, adminId={}, username={}", admin.getId(), admin.getUsername());
        return response;
    }
}
