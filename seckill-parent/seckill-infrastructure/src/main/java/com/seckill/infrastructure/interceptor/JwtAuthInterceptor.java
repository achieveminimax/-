package com.seckill.infrastructure.interceptor;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
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

    // 白名单路径
    private static final String[] WHITE_LIST = {
            "/api/user/register",
            "/api/user/login",
            "/api/user/refresh-token",
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

        // 检查 Token 是否在黑名单中（用户已登出）
        if (redisUtils.hasKey(TOKEN_BLACKLIST_KEY + token)) {
            log.warn("Token 已被加入黑名单: {}", requestUri);
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED, "Token 已失效，请重新登录");
        }

        // 从 Token 中获取用户ID
        Long userId = JwtUtils.getUserIdFromToken(token);

        // 将用户ID存入 ThreadLocal
        UserContext.setCurrentUserId(userId);

        log.debug("JWT 认证通过, userId: {}, uri: {}", userId, requestUri);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal
        UserContext.clear();
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
        return false;
    }
}
