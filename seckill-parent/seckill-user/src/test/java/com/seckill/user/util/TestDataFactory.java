package com.seckill.user.util;

import com.seckill.user.dto.*;
import com.seckill.user.entity.Address;
import com.seckill.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 测试数据工厂
 * 提供创建测试实体和 DTO 的方法
 */
public class TestDataFactory {

    // ==================== 用户相关 ====================

    /**
     * 创建测试用户实体
     */
    public static User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("$2a$10$wRHKFMtOlHREz5RqLQMt7OqFPGvLGQB5R5PzJ5dX3Vv9k8R5J5dK"); // BCrypt encoded password
        user.setNickname("测试用户");
        user.setPhone("13800138000");
        user.setAvatar("https://example.com/avatar.jpg");
        user.setGender(1);
        user.setEmail("test@example.com");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        return user;
    }

    /**
     * 创建测试用户实体（自定义ID和用户名）
     */
    public static User createUser(Long id, String username) {
        User user = createUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    /**
     * 创建用户注册请求
     */
    public static UserRegisterRequest createUserRegisterRequest() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Test123456");
        request.setConfirmPassword("Test123456");
        request.setPhone("13900139000");
        request.setVerifyCode("888888");
        request.setEmail("new@example.com");
        request.setNickname("新用户");
        return request;
    }

    /**
     * 创建用户登录请求
     */
    public static UserLoginRequest createUserLoginRequest() {
        UserLoginRequest request = new UserLoginRequest();
        request.setAccount("testuser");
        request.setPassword("Test123456");
        return request;
    }

    /**
     * 创建用户登录请求（手机号）
     */
    public static UserLoginRequest createUserLoginRequestWithPhone() {
        UserLoginRequest request = new UserLoginRequest();
        request.setAccount("13800138000");
        request.setPassword("Test123456");
        return request;
    }

    /**
     * 创建用户信息更新请求
     */
    public static UserUpdateRequest createUserUpdateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("更新后的昵称");
        request.setGender("2");
        request.setBirthday(LocalDate.of(1995, 5, 5));
        request.setEmail("updated@example.com");
        return request;
    }

    /**
     * 创建密码更新请求
     */
    public static PasswordUpdateRequest createPasswordUpdateRequest() {
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setOldPassword("Test123456");
        request.setNewPassword("NewPass123");
        request.setConfirmPassword("NewPass123");
        return request;
    }

    /**
     * 创建密码更新请求（旧密码错误）
     */
    public static PasswordUpdateRequest createPasswordUpdateRequestWrongOld() {
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setOldPassword("WrongPassword");
        request.setNewPassword("NewPass123");
        request.setConfirmPassword("NewPass123");
        return request;
    }

    /**
     * 创建 Token 刷新请求
     */
    public static TokenRefreshRequest createTokenRefreshRequest(String refreshToken) {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    // ==================== 地址相关 ====================

    /**
     * 创建测试地址实体
     */
    public static Address createAddress() {
        Address address = new Address();
        address.setId(1L);
        address.setUserId(1L);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetailAddress("科技园南区xxx栋xxx室");
        address.setIsDefault(1);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        address.setDeleted(0);
        return address;
    }

    /**
     * 创建测试地址实体（自定义ID和用户ID）
     */
    public static Address createAddress(Long id, Long userId) {
        Address address = createAddress();
        address.setId(id);
        address.setUserId(userId);
        return address;
    }

    /**
     * 创建地址请求
     */
    public static AddressRequest createAddressRequest() {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("李四");
        request.setReceiverPhone("13900139000");
        request.setProvince("广东省");
        request.setCity("广州市");
        request.setDistrict("天河区");
        request.setDetailAddress("天河路xxx号xxx室");
        request.setIsDefault(0);
        return request;
    }

    /**
     * 创建默认地址请求
     */
    public static AddressRequest createDefaultAddressRequest() {
        AddressRequest request = createAddressRequest();
        request.setIsDefault(1);
        return request;
    }

    // ==================== 常量 ====================

    public static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjF9.test-signature";
    public static final String TEST_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjF9.test-refresh-signature";
}
