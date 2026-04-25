package com.seckill.admin.service;

import com.seckill.admin.dto.AdminLoginRequest;
import com.seckill.admin.dto.AdminLoginResponse;
import com.seckill.admin.entity.Admin;
import com.seckill.admin.mapper.AdminMapper;
import com.seckill.admin.service.impl.AdminAuthServiceImpl;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthService 单元测试")
class AdminAuthServiceUnitTest {

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AdminAuthServiceImpl adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthServiceImpl(adminMapper, redisUtils, passwordEncoder);
    }

    @Test
    @DisplayName("管理员登录成功 - 返回 token 并更新最后登录时间")
    void login_Success() {
        Admin admin = admin();
        AdminLoginRequest request = request();

        when(adminMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches(request.getPassword(), admin.getPassword())).thenReturn(true);

        AdminLoginResponse response = adminAuthService.login(request);

        assertNotNull(response);
        assertEquals(admin.getId(), response.getAdminId());
        assertEquals(admin.getRole(), response.getRole());
        assertNotNull(response.getRefreshToken());
        verify(redisUtils).set(eq("admin:token:" + admin.getId()), eq(response.getToken()), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisUtils).set(eq("admin:refresh:" + admin.getId()), eq(response.getRefreshToken()), anyLong(), eq(TimeUnit.SECONDS));

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getLastLoginTime());
    }

    @Test
    @DisplayName("管理员登录失败 - 密码错误")
    void login_Fail_InvalidPassword() {
        Admin admin = admin();
        AdminLoginRequest request = request();

        when(adminMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches(request.getPassword(), admin.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminAuthService.login(request));

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
    }

    private Admin admin() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("$2a$10$encoded");
        admin.setRealName("系统管理员");
        admin.setRole("SUPER_ADMIN");
        admin.setStatus(1);
        return admin;
    }

    private AdminLoginRequest request() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");
        return request;
    }
}
