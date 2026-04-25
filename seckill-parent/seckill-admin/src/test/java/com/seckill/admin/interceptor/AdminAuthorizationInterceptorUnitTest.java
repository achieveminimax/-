package com.seckill.admin.interceptor;

import com.seckill.admin.annotation.RequireAdmin;
import com.seckill.admin.enums.AdminRoleEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.AdminContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthorizationInterceptor 单元测试")
class AdminAuthorizationInterceptorUnitTest {

    private AdminAuthorizationInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new AdminAuthorizationInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        AdminContext.clear();
    }

    @AfterEach
    void tearDown() {
        AdminContext.clear();
    }

    @Test
    @DisplayName("非HandlerMethod直接放行")
    void preHandle_Skip_NonHandlerMethod() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    @DisplayName("角色无效 - 抛出异常")
    void preHandle_Fail_InvalidRole() {
        AdminContext.setCurrentRole("INVALID_ROLE");
        HandlerMethod handler = handlerForMethod("superAdminOnly");

        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    @DisplayName("角色不在允许列表 - 抛出异常")
    void preHandle_Fail_RoleNotAllowed() {
        AdminContext.setCurrentRole(AdminRoleEnum.OPERATOR.name());
        HandlerMethod handler = handlerForMethod("superAdminOnly");

        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    @DisplayName("SUPER_ADMIN角色匹配通过")
    void preHandle_Success_AllowedRole() throws Exception {
        AdminContext.setCurrentRole(AdminRoleEnum.SUPER_ADMIN.name());
        HandlerMethod handler = handlerForMethod("superAdminOnly");

        boolean result = interceptor.preHandle(request, response, handler);
        assertTrue(result);
    }

    @Test
    @DisplayName("无注解时默认要求SUPER_ADMIN - ADMIN被拒绝")
    void preHandle_Fail_DefaultRequireSuperAdmin() {
        AdminContext.setCurrentRole(AdminRoleEnum.ADMIN.name());
        HandlerMethod handler = handlerForMethod("noAnnotation");

        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
    }

    @Test
    @DisplayName("无注解时默认要求SUPER_ADMIN - SUPER_ADMIN通过")
    void preHandle_Success_DefaultSuperAdmin() throws Exception {
        AdminContext.setCurrentRole(AdminRoleEnum.SUPER_ADMIN.name());
        HandlerMethod handler = handlerForMethod("noAnnotation");

        boolean result = interceptor.preHandle(request, response, handler);
        assertTrue(result);
    }

    @Test
    @DisplayName("多角色注解 - OPERATOR在允许列表中通过")
    void preHandle_Success_MultipleAllowedRoles() throws Exception {
        AdminContext.setCurrentRole(AdminRoleEnum.OPERATOR.name());
        HandlerMethod handler = handlerForMethod("adminAndOperator");

        boolean result = interceptor.preHandle(request, response, handler);
        assertTrue(result);
    }

    @Test
    @DisplayName("ADMIN角色不在只允许OPERATOR的列表中")
    void preHandle_Fail_AdminNotAllowedForOperatorOnly() {
        AdminContext.setCurrentRole(AdminRoleEnum.ADMIN.name());
        HandlerMethod handler = handlerForMethod("operatorOnly");

        assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
    }

    private HandlerMethod handlerForMethod(String methodName) {
        try {
            java.lang.reflect.Method method = TestController.class.getDeclaredMethod(methodName);
            return new HandlerMethod(new TestController(), method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static class TestController {
        @RequireAdmin(roles = AdminRoleEnum.SUPER_ADMIN)
        public void superAdminOnly() {
        }

        @RequireAdmin(roles = {AdminRoleEnum.SUPER_ADMIN, AdminRoleEnum.OPERATOR})
        public void adminAndOperator() {
        }

        @RequireAdmin(roles = AdminRoleEnum.OPERATOR)
        public void operatorOnly() {
        }

        public void noAnnotation() {
        }
    }
}
