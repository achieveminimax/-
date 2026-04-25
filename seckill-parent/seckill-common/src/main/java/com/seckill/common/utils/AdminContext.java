package com.seckill.common.utils;

/**
 * 管理员上下文工具类
 * 使用 ThreadLocal 存储当前管理员信息
 */
public class AdminContext {

    private static final ThreadLocal<Long> ADMIN_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    /**
     * 设置当前管理员 ID
     */
    public static void setCurrentAdminId(Long adminId) {
        ADMIN_ID_HOLDER.set(adminId);
    }

    /**
     * 获取当前管理员 ID
     */
    public static Long getCurrentAdminId() {
        return ADMIN_ID_HOLDER.get();
    }

    /**
     * 设置当前角色
     */
    public static void setCurrentRole(String role) {
        ROLE_HOLDER.set(role);
    }

    /**
     * 获取当前角色
     */
    public static String getCurrentRole() {
        return ROLE_HOLDER.get();
    }

    /**
     * 清理上下文
     */
    public static void clear() {
        ADMIN_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
