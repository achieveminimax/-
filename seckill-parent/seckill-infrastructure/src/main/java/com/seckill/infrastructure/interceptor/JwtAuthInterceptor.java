package com.seckill.infrastructure.interceptor;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.AdminContext;
import com.seckill.common.utils.JwtUtils;
import com.seckill.common.utils.UserContext;
import com.seckill.infrastructure.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final RedisUtils redisUtils;

    // Token 黑名单 Key 前缀
    private static final String TOKEN_BLACKLIST_KEY = "token:blacklist:";
    private static final String USER_TOKEN_KEY = "user:token:";
    private static final String ADMIN_TOKEN_KEY = "admin:token:";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    // 白名单路径
    private static final String[] WHITE_LIST = {
            "/api/user/register",
            "/api/user/login",
            "/api/user/refresh-token",
            "/api/admin/login",
            "/api/category/list",
            "/api/goods/list",
            "/api/seckill/list",
            "/doc.html",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/",
            "/favicon.ico",
            "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();

        // 检查是否是白名单路径
        if (isWhiteList(requestUri)) {
            return true;
        }

        // 从请求头中获取 Authorization
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("请求缺少有效的 Authorization 头: {}", requestUri);
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "请先登录");
        }

        // 提取 Token
        String token = authorization.substring(7);

        // 验证 Token 是否有效
        if (!JwtUtils.validateToken(token)) {
            log.warn("Token 无效或已过期: {}", requestUri);
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Token 无效或已过期");
        }

        if (!JwtUtils.isAccessToken(token)) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "当前接口仅支持访问 Token");
        }

        // 检查 Token 是否在黑名单中（用户已登出）
        if (Boolean.TRUE.equals(redisUtils.hasKey(TOKEN_BLACKLIST_KEY + token))) {
            log.warn("Token 已被加入黑名单: {}", requestUri);
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Token 已失效，请重新登录");
        }

        Long principalId = JwtUtils.getUserIdFromToken(token);
        String subjectType = JwtUtils.getSubjectType(token);
        String storedToken;

        if (requestUri.startsWith(ADMIN_PATH_PREFIX)) {
            if (!"admin".equals(subjectType)) {
                throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "管理员接口需要管理员身份");
            }
            storedToken = redisUtils.get(ADMIN_TOKEN_KEY + principalId);
            if (storedToken == null || !storedToken.equals(token)) {
                throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "管理员登录状态已失效");
            }
            AdminContext.setCurrentAdminId(principalId);
            AdminContext.setCurrentRole(JwtUtils.getRole(token));
            log.debug("管理员认证通过, adminId: {}, uri: {}", principalId, requestUri);
            return true;
        }

        if (!"user".equals(subjectType)) {
            throw new BusinessException(ResponseCodeEnum.FORBIDDEN, "当前接口需要用户身份");
        }

        storedToken = redisUtils.get(USER_TOKEN_KEY + principalId);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "用户登录状态已失效");
        }

        UserContext.setCurrentUserId(principalId);

        log.debug("JWT 认证通过, userId: {}, uri: {}", principalId, requestUri);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal
        UserContext.clear();
        AdminContext.clear();
    }

    /**
     * 检查是否是白名单路径
     */
    private boolean isWhiteList(String requestUri) {
        for (String whitePath : WHITE_LIST) {
            if (requestUri.startsWith(whitePath) || requestUri.equals(whitePath)) {
                return true;
            }
        }
        if (requestUri.matches("^/api/goods/\\d+$")) {
            return true;
        }
        if (requestUri.matches("^/api/seckill/\\d+$")) {
            return true;
        }
        return false;
    }
}
