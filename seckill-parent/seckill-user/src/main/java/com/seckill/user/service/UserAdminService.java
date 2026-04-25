package com.seckill.user.service;

/**
 * 管理端用户服务。
 */
public interface UserAdminService {

    /**
     * 更新用户状态。
     *
     * @param userId 用户 ID
     * @param status 目标状态
     */
    void updateUserStatus(Long userId, Integer status);
}
