package com.seckill.admin.annotation;

import com.seckill.admin.enums.AdminRoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明管理端接口所需角色。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {

    /**
     * 允许访问当前接口的角色集合。
     */
    AdminRoleEnum[] roles() default {AdminRoleEnum.SUPER_ADMIN};
}
