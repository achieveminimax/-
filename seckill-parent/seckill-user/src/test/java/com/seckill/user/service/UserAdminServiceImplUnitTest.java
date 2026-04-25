package com.seckill.user.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.user.entity.User;
import com.seckill.user.mapper.UserMapper;
import com.seckill.user.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdminServiceImpl 单元测试")
class UserAdminServiceImplUnitTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisUtils redisUtils;

    private UserAdminServiceImpl userAdminService;

    @BeforeEach
    void setUp() {
        userAdminService = new UserAdminServiceImpl(userMapper, redisUtils);
    }

    @Test
    @DisplayName("更新用户状态 - status为null抛出异常")
    void updateUserStatus_NullStatus_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userAdminService.updateUserStatus(1L, null));
        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新用户状态 - status非法值抛出异常")
    void updateUserStatus_InvalidStatus_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userAdminService.updateUserStatus(1L, 2));
        assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新用户状态 - 用户不存在抛出异常")
    void updateUserStatus_UserNotFound_ThrowsBusinessException() {
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userAdminService.updateUserStatus(1L, 0));
        assertEquals(ResponseCodeEnum.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新用户状态 - 禁用用户成功并清除缓存")
    void updateUserStatus_DisableSuccess_ClearsCaches() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        userAdminService.updateUserStatus(1L, 0);

        assertEquals(0, user.getStatus());
        verify(userMapper).updateById(user);
        verify(redisUtils).delete("user:token:1");
        verify(redisUtils).delete("user:refresh:1");
        verify(redisUtils).delete("user:info:1");
    }

    @Test
    @DisplayName("更新用户状态 - 启用用户成功并清除缓存")
    void updateUserStatus_EnableSuccess_ClearsCaches() {
        User user = new User();
        user.setId(1L);
        user.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        userAdminService.updateUserStatus(1L, 1);

        assertEquals(1, user.getStatus());
        verify(userMapper).updateById(user);
        verify(redisUtils).delete("user:token:1");
        verify(redisUtils).delete("user:refresh:1");
        verify(redisUtils).delete("user:info:1");
    }
}
