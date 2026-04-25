package com.seckill.user.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.JwtUtils;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.user.dto.*;
import com.seckill.user.entity.User;
import com.seckill.user.mapper.UserMapper;
import com.seckill.user.service.impl.UserServiceImpl;
import com.seckill.user.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 纯单元测试
 * 使用纯 Mockito，避免 Spring Boot 的复杂依赖
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceUnitTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, redisUtils, passwordEncoder);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册失败 - 密码不一致")
    void register_PasswordMismatch() {
        // Given
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();
        request.setConfirmPassword("DifferentPassword");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("密码不一致"));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_UsernameExists() {
        // Given
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();

        when(userMapper.countByUsername(request.getUsername())).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });

        assertEquals(ResponseCodeEnum.USERNAME_EXISTS.getCode(), exception.getCode());
        verify(userMapper).countByUsername(request.getUsername());
    }

    @Test
    @DisplayName("注册失败 - 手机号已存在")
    void register_PhoneExists() {
        // Given
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();

        when(userMapper.countByUsername(request.getUsername())).thenReturn(0);
        when(userMapper.countByPhone(request.getPhone())).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });

        assertEquals(ResponseCodeEnum.PHONE_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("注册失败 - 验证码错误")
    void register_InvalidVerifyCode() {
        // Given
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();
        request.setVerifyCode("123456");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });

        assertEquals(ResponseCodeEnum.VERIFY_CODE_ERROR.getCode(), exception.getCode());
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_UserNotFound() {
        // Given
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.login(request);
        });

        assertEquals(ResponseCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("登录失败 - 账号被禁用")
    void login_AccountDisabled() {
        // Given
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();
        User user = TestDataFactory.createUser();
        user.setStatus(0); // 禁用状态

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(user);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.login(request);
        });

        assertEquals(ResponseCodeEnum.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("账号已被禁用"));
    }

    @Test
    @DisplayName("登录成功 - 更新最后登录时间并保存 Token")
    void login_Success_UpdateLastLoginAndStoreTokens() {
        // Given
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        // When
        UserLoginResponse response = userService.login(request);

        // Then
        assertNotNull(response);
        assertEquals(user.getId(), response.getUserId());
        assertEquals(request.getAccount(), response.getAccount());
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());

        verify(redisUtils).hasKey("user:lock:" + user.getId());
        verify(redisUtils, never()).hasKey("user:lock:" + request.getAccount());
        verify(redisUtils).delete("login:fail:" + user.getId());
        verify(redisUtils).set(eq("user:token:" + user.getId()), eq(response.getToken()), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisUtils).set(eq("user:refresh:" + user.getId()), eq(response.getRefreshToken()), anyLong(), eq(TimeUnit.SECONDS));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertNotNull(userCaptor.getValue().getLockTime());
    }

    // ==================== 个人信息管理测试 ====================

    @Test
    @DisplayName("获取用户信息成功")
    void getUserInfo_Success() {
        // Given
        Long userId = 1L;
        User user = TestDataFactory.createUser();

        when(userMapper.selectById(userId)).thenReturn(user);

        // When
        UserInfoResponse response = userService.getUserInfo(userId);

        // Then
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals(user.getNickname(), response.getNickname());
        assertTrue(response.getPhone().contains("****")); // 验证手机号脱敏
    }

    @Test
    @DisplayName("获取用户信息失败 - 用户不存在")
    void getUserInfo_UserNotFound() {
        // Given
        Long userId = 999L;

        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserInfo(userId);
        });

        assertEquals(ResponseCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("更新用户信息成功")
    void updateUserInfo_Success() {
        // Given
        Long userId = 1L;
        UserUpdateRequest request = TestDataFactory.createUserUpdateRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectById(userId)).thenReturn(user);

        // When
        assertDoesNotThrow(() -> userService.updateUserInfo(userId, request));

        // Then
        assertEquals(request.getNickname(), user.getNickname());
        assertEquals(Integer.parseInt(request.getGender()), user.getGender());
        assertEquals(request.getBirthday(), user.getBirthday());
        assertEquals(request.getEmail(), user.getEmail());
        verify(userMapper).updateById(user);
    }

    @Test
    @DisplayName("更新用户信息失败 - 性别参数非法")
    void updateUserInfo_InvalidGender() {
        // Given
        Long userId = 1L;
        UserUpdateRequest request = TestDataFactory.createUserUpdateRequest();
        request.setGender("x");

        when(userMapper.selectById(userId)).thenReturn(TestDataFactory.createUser());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUserInfo(userId, request);
        });

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("gender"));
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void updatePassword_WrongOldPassword() {
        // Given
        Long userId = 1L;
        PasswordUpdateRequest request = TestDataFactory.createPasswordUpdateRequestWrongOld();
        User user = TestDataFactory.createUser();

        when(userMapper.selectById(userId)).thenReturn(user);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updatePassword(userId, request);
        });

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("原密码错误"));
    }

    @Test
    @DisplayName("修改密码失败 - 新密码与旧密码相同")
    void updatePassword_SameAsOld() {
        // Given
        Long userId = 1L;
        PasswordUpdateRequest request = TestDataFactory.createPasswordUpdateRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updatePassword(userId, request);
        });

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("新密码不能与旧密码相同"));
    }

    // ==================== Token 刷新测试 ====================

    @Test
    @DisplayName("刷新 Token 失败 - Token 类型错误")
    void refreshToken_TypeError() {
        String token = JwtUtils.generateAccessToken(1L);
        TokenRefreshRequest request = TestDataFactory.createTokenRefreshRequest(token);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.refreshToken(request));

        assertEquals(ResponseCodeEnum.UNAUTHORIZED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("类型错误"));
    }

    // ==================== 补充测试 ====================

    @Test
    @DisplayName("注册成功 - 用户名和手机号均可用")
    void register_Success() {
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();

        when(userMapper.countByUsername(request.getUsername())).thenReturn(0);
        when(userMapper.countByPhone(request.getPhone())).thenReturn(0);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");

        var response = userService.register(request);

        assertNotNull(response);
        assertEquals(request.getUsername(), response.getUsername());
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("登录成功 - 手机号登录")
    void login_Success_WithPhone() {
        UserLoginRequest request = TestDataFactory.createUserLoginRequestWithPhone();
        User user = TestDataFactory.createUser();

        when(userMapper.selectByPhone(request.getAccount())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        UserLoginResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals(user.getId(), response.getUserId());
        verify(userMapper).selectByPhone(request.getAccount());
    }

    @Test
    @DisplayName("登录失败 - 账号被锁定")
    void login_AccountLocked() {
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(user);
        when(redisUtils.hasKey("user:lock:" + user.getId())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(request));

        assertEquals(ResponseCodeEnum.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("锁定"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误，记录失败次数")
    void login_WrongPassword_RecordFailCount() {
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);
        when(redisUtils.increment(anyString(), anyLong())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(request));

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        verify(redisUtils).increment("login:fail:" + user.getId(), 1);
    }

    @Test
    @DisplayName("登录失败 - 连续失败5次后锁定账号")
    void login_FailFiveTimes_AccountLocked() {
        UserLoginRequest request = TestDataFactory.createUserLoginRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectByUsername(request.getAccount())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);
        when(redisUtils.increment(anyString(), anyLong())).thenReturn(5L);

        assertThrows(BusinessException.class, () -> userService.login(request));

        verify(redisUtils).set(eq("user:lock:" + user.getId()), eq("1"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("登出 - 将 Token 加入黑名单并清除用户 Token")
    void logout_Success() {
        Long userId = 1L;
        String token = JwtUtils.generateAccessToken(userId);

        userService.logout(token, userId);

        verify(redisUtils).set(startsWith("token:blacklist:"), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(redisUtils).delete("user:token:" + userId);
        verify(redisUtils).delete("user:refresh:" + userId);
    }

    @Test
    @DisplayName("修改密码成功")
    void updatePassword_Success() {
        Long userId = 1L;
        PasswordUpdateRequest request = TestDataFactory.createPasswordUpdateRequest();
        User user = TestDataFactory.createUser();

        when(userMapper.selectById(userId)).thenReturn(user);
        when(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("new_encoded_password");

        assertDoesNotThrow(() -> userService.updatePassword(userId, request));

        verify(userMapper).updateById(user);
        verify(redisUtils).delete("user:token:" + userId);
        verify(redisUtils).delete("user:refresh:" + userId);
    }

    @Test
    @DisplayName("修改密码失败 - 新密码与确认密码不一致")
    void updatePassword_NewPasswordMismatch() {
        Long userId = 1L;
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("NewPass123");
        request.setConfirmPassword("DifferentPass123");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(userId, request));

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("密码不一致"));
    }

    @Test
    @DisplayName("修改密码失败 - 用户不存在")
    void updatePassword_UserNotFound() {
        Long userId = 999L;
        PasswordUpdateRequest request = TestDataFactory.createPasswordUpdateRequest();

        when(userMapper.selectById(userId)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(userId, request));

        assertEquals(ResponseCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("更新用户信息失败 - 用户不存在")
    void updateUserInfo_UserNotFound() {
        Long userId = 999L;
        UserUpdateRequest request = TestDataFactory.createUserUpdateRequest();

        when(userMapper.selectById(userId)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUserInfo(userId, request));

        assertEquals(ResponseCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("更新用户信息失败 - 性别参数超出范围")
    void updateUserInfo_GenderOutOfRange() {
        Long userId = 1L;
        UserUpdateRequest request = TestDataFactory.createUserUpdateRequest();
        request.setGender("5");

        when(userMapper.selectById(userId)).thenReturn(TestDataFactory.createUser());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUserInfo(userId, request));

        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("gender"));
    }

    @Test
    @DisplayName("获取用户信息 - 手机号脱敏验证")
    void getUserInfo_PhoneMasked() {
        Long userId = 1L;
        User user = TestDataFactory.createUser();
        user.setPhone("13800138000");

        when(userMapper.selectById(userId)).thenReturn(user);

        UserInfoResponse response = userService.getUserInfo(userId);

        assertEquals("138****8000", response.getPhone());
    }

    @Test
    @DisplayName("注册成功 - 昵称为空时使用用户名")
    void register_Success_NicknameFallback() {
        UserRegisterRequest request = TestDataFactory.createUserRegisterRequest();
        request.setNickname("");

        when(userMapper.countByUsername(request.getUsername())).thenReturn(0);
        when(userMapper.countByPhone(request.getPhone())).thenReturn(0);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");

        var response = userService.register(request);

        assertEquals(request.getUsername(), response.getNickname());
    }
}
