package com.seckill.admin.interceptor;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.AdminContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 管理端角色授权拦截器。
 */
@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAdmin requireAdmin = resolveAnnotation(handlerMethod);
        if (requireAdmin == null) {
            requireAdmin = defaultRequireAdmin();
        }

        AdminRoleEnum currentRole = AdminRoleEnum.fromCode(AdminContext.getCurrentRole());
        if (currentRole == null) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "管理员角色无效");
        }

        Set<AdminRoleEnum> allowedRoles = EnumSet.copyOf(Arrays.asList(requireAdmin.roles()));
        if (!allowedRoles.contains(currentRole)) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "当前角色无权访问该管理接口");
        }
        return true;
    }

    private RequireAdmin resolveAnnotation(HandlerMethod handlerMethod) {
        RequireAdmin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAdmin.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAdmin.class);
    }

    private RequireAdmin defaultRequireAdmin() {
        return new RequireAdmin() {
            @Override
            public Class<RequireAdmin> annotationType() {
                return RequireAdmin.class;
            }

            @Override
            public AdminRoleEnum[] roles() {
                return new AdminRoleEnum[]{AdminRoleEnum.SUPER_ADMIN};
            }
        };
    }
}
