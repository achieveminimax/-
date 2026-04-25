package com.seckill.user.service.impl;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.user.entity.User;
import com.seckill.user.mapper.UserMapper;
import com.seckill.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final String USER_TOKEN_KEY = "user:token:";
    private static final String USER_REFRESH_KEY = "user:refresh:";
    private static final String USER_INFO_KEY = "user:info:";

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "用户状态仅支持 0-禁用 或 1-启用");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        user.setStatus(status);
        userMapper.updateById(user);

        redisUtils.delete(USER_TOKEN_KEY + userId);
        redisUtils.delete(USER_REFRESH_KEY + userId);
        redisUtils.delete(USER_INFO_KEY + userId);
    }
}
