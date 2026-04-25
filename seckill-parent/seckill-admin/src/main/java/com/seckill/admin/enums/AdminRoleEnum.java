package com.seckill.admin.enums;

import java.util.Arrays;

/**
 * 管理员角色枚举。
 */
public enum AdminRoleEnum {
    SUPER_ADMIN,
    ADMIN,
    OPERATOR;

    public static AdminRoleEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
