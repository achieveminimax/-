package com.seckill.user.service.impl;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.JwtUtils;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.user.dto.*;
import com.seckill.user.entity.User;
import com.seckill.user.mapper.UserMapper;
import com.seckill.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    // Token 有效期（秒）
    private static final long ACCESS_TOKEN_EXPIRE = 30 * 60; // 30分钟
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60; // 7天

    // Redis Key 前缀
    private static final String USER_TOKEN_KEY = "user:token:";
    private static final String USER_REFRESH_KEY = "user:refresh:";
    private static final String TOKEN_BLACKLIST_KEY = "token:blacklist:";
    private static final String LOGIN_FAIL_KEY = "login:fail:";
    private static final String USER_LOCK_KEY = "user:lock:";

    // 登录失败最大次数和锁定时间
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final long LOCK_TIME_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterResponse register(UserRegisterRequest request) {
        // 校验密码和确认密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 校验验证码（模拟，固定值888888）
        if (!"888888".equals(request.getVerifyCode())) {
            throw new BusinessException(ResponseCodeEnum.VERIFY_CODE_ERROR);
        }

        // 检查用户名是否已存在
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ResponseCodeEnum.USERNAME_EXISTS);
        }

        // 检查手机号是否已存在
        if (userMapper.countByPhone(request.getPhone()) > 0) {
            throw new BusinessException(ResponseCodeEnum.PHONE_EXISTS);
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setGender(0);
        user.setStatus(1);
        user.setLoginFailCount(0);

        // 保存用户
        userMapper.insert(user);

        // 生成 Token
        String token = JwtUtils.generateAccessToken(user.getId());
        String refreshToken = JwtUtils.generateRefreshToken(user.getId());

        // 保存 Token 到 Redis
        redisUtils.set(USER_TOKEN_KEY + user.getId(), token, ACCESS_TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisUtils.set(USER_REFRESH_KEY + user.getId(), refreshToken, REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);

        // 构建响应
        UserRegisterResponse response = new UserRegisterResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(ACCESS_TOKEN_EXPIRE);

        log.info("用户注册成功: {}", user.getUsername());
        return response;
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        String account = request.getAccount();

        // 根据账号查询用户（支持用户名或手机号登录）
        User user;
        if (account.matches("^1[3-9]\\d{9}$")) {
            // 手机号登录
            user = userMapper.selectByPhone(account);
        } else {
            // 用户名登录
            user = userMapper.selectByUsername(account);
        }

        // 用户不存在
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 检查账号是否被锁定
        String lockKey = USER_LOCK_KEY + user.getId();
        if (Boolean.TRUE.equals(redisUtils.hasKey(lockKey))) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "账号已锁定，请" + LOCK_TIME_MINUTES + "分钟后重试");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 记录登录失败次数
            recordLoginFail(user.getId());
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "账号或密码错误");
        }

        // 登录成功，清除失败记录
        clearLoginFail(user.getId());

        // 更新最后登录时间
        user.setLoginFailCount(0);
        user.setLockTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 Token
        String token = JwtUtils.generateAccessToken(user.getId());
        String refreshToken = JwtUtils.generateRefreshToken(user.getId());

        // 保存 Token 到 Redis
        redisUtils.set(USER_TOKEN_KEY + user.getId(), token, ACCESS_TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisUtils.set(USER_REFRESH_KEY + user.getId(), refreshToken, REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);

        // 构建响应
        UserLoginResponse response = new UserLoginResponse();
        response.setUserId(user.getId());
        response.setAccount(account);
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(ACCESS_TOKEN_EXPIRE);

        log.info("用户登录成功: {}", user.getUsername());
        return response;
    }

    @Override
    public void logout(String token, Long userId) {
        // 将 Token 加入黑名单
        long remainingTime = JwtUtils.getRemainingTime(token);
        if (remainingTime > 0) {
            redisUtils.set(TOKEN_BLACKLIST_KEY + token, "1", remainingTime, TimeUnit.MILLISECONDS);
        }

        // 清除用户 Token
        redisUtils.delete(USER_TOKEN_KEY + userId);
        redisUtils.delete(USER_REFRESH_KEY + userId);

        log.info("用户登出成功: {}", userId);
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 验证 Refresh Token 是否有效
        if (!JwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Refresh Token 无效或已过期");
        }

        Claims claims = JwtUtils.parseToken(refreshToken);
        Object tokenType = claims.get("type");
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Refresh Token 类型错误");
        }

        // 检查是否在黑名单中
        if (Boolean.TRUE.equals(redisUtils.hasKey(TOKEN_BLACKLIST_KEY + refreshToken))) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Refresh Token 已失效");
        }

        // 获取用户ID
        Long userId = Long.valueOf(claims.getSubject());

        // 验证 Redis 中的 Refresh Token 是否一致
        String storedRefreshToken = (String) redisUtils.get(USER_REFRESH_KEY + userId);
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Refresh Token 已失效");
        }

        // 将旧的 Refresh Token 加入黑名单
        long remainingTime = JwtUtils.getRemainingTime(refreshToken);
        if (remainingTime > 0) {
            redisUtils.set(TOKEN_BLACKLIST_KEY + refreshToken, "1", remainingTime, TimeUnit.MILLISECONDS);
        }

        // 生成新的 Token
        String newToken = JwtUtils.generateAccessToken(userId);
        String newRefreshToken = JwtUtils.generateRefreshToken(userId);

        // 保存新的 Token 到 Redis
        redisUtils.set(USER_TOKEN_KEY + userId, newToken, ACCESS_TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisUtils.set(USER_REFRESH_KEY + userId, newRefreshToken, REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);

        // 构建响应
        TokenRefreshResponse response = new TokenRefreshResponse();
        response.setToken(newToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(ACCESS_TOKEN_EXPIRE);

        log.info("Token 刷新成功: {}", userId);
        return response;
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        // 手机号脱敏
        response.setPhone(maskPhone(user.getPhone()));
        response.setEmail(user.getEmail());
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        response.setLastLoginTime(user.getLockTime()); // 使用 lockTime 字段存储最后登录时间

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 更新字段
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getGender())) {
            int gender;
            try {
                gender = Integer.parseInt(request.getGender());
            } catch (NumberFormatException e) {
                throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "gender 参数非法");
            }
            if (gender < 0 || gender > 2) {
                throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "gender 参数非法");
            }
            user.setGender(gender);
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        userMapper.updateById(user);
        log.info("用户信息更新成功: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        // 校验新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "原密码错误");
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        // 使所有 Token 失效（强制重新登录）
        redisUtils.delete(USER_TOKEN_KEY + userId);
        redisUtils.delete(USER_REFRESH_KEY + userId);

        log.info("用户密码更新成功: {}", userId);
    }

    /**
     * 记录登录失败次数
     */
    private void recordLoginFail(Long userId) {
        String failKey = LOGIN_FAIL_KEY + userId;
        Long count = redisUtils.increment(failKey, 1);
        redisUtils.expire(failKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);

        if (count != null && count >= MAX_LOGIN_FAIL_COUNT) {
            // 锁定账号
            redisUtils.set(USER_LOCK_KEY + userId, "1", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            log.warn("账号被锁定: {}, 失败次数: {}", userId, count);
        }
    }

    /**
     * 清除登录失败记录
     */
    private void clearLoginFail(Long userId) {
        redisUtils.delete(LOGIN_FAIL_KEY + userId);
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
