package com.seckill.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
public class JwtUtils {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String SUBJECT_TYPE_CLAIM = "subjectType";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String USER_SUBJECT_TYPE = "user";
    private static final String ADMIN_SUBJECT_TYPE = "admin";

    /**
     * 默认密钥（生产环境应从配置文件读取）
     */
    private static final String DEFAULT_SECRET = "seckill-system-jwt-secret-key-2024-secure-key";

    /**
     * 访问Token有效期（30分钟）
     */
    private static final long ACCESS_TOKEN_EXPIRE = 30 * 60 * 1000;

    /**
     * 刷新Token有效期（7天）
     */
    private static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60 * 1000;

    /**
     * 获取签名密钥
     */
    private static SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问Token
     *
     * @param userId 用户ID
     * @return Token字符串
     */
    public static String generateAccessToken(Long userId) {
        return generateAccessToken(userId, DEFAULT_SECRET);
    }

    /**
     * 生成访问Token
     *
     * @param userId 用户ID
     * @param secret 密钥
     * @return Token字符串
     */
    public static String generateAccessToken(Long userId, String secret) {
        return generateToken(userId, ACCESS_TOKEN_EXPIRE, secret, USER_SUBJECT_TYPE, null);
    }

    /**
     * 生成刷新Token
     *
     * @param userId 用户ID
     * @return Token字符串
     */
    public static String generateRefreshToken(Long userId) {
        return generateRefreshToken(userId, DEFAULT_SECRET);
    }

    /**
     * 生成刷新Token
     *
     * @param userId 用户ID
     * @param secret 密钥
     * @return Token字符串
     */
    public static String generateRefreshToken(Long userId, String secret) {
        return generateToken(userId, REFRESH_TOKEN_EXPIRE, secret, USER_SUBJECT_TYPE, null);
    }

    /**
     * 生成管理员访问 Token
     */
    public static String generateAdminAccessToken(Long adminId, String role) {
        return generateAdminAccessToken(adminId, role, DEFAULT_SECRET);
    }

    /**
     * 生成管理员访问 Token
     */
    public static String generateAdminAccessToken(Long adminId, String role, String secret) {
        return generateToken(adminId, ACCESS_TOKEN_EXPIRE, secret, ADMIN_SUBJECT_TYPE, role);
    }

    /**
     * 生成管理员刷新 Token
     */
    public static String generateAdminRefreshToken(Long adminId) {
        return generateAdminRefreshToken(adminId, DEFAULT_SECRET);
    }

    /**
     * 生成管理员刷新 Token
     */
    public static String generateAdminRefreshToken(Long adminId, String secret) {
        return generateToken(adminId, REFRESH_TOKEN_EXPIRE, secret, ADMIN_SUBJECT_TYPE, null);
    }

    /**
     * 生成Token
     *
     * @param userId     用户ID
     * @param expireTime 过期时间（毫秒）
     * @param secret     密钥
     * @return Token字符串
     */
    public static String generateToken(Long userId, long expireTime, String secret, String subjectType, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(TOKEN_TYPE_CLAIM, expireTime == ACCESS_TOKEN_EXPIRE ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE);
        claims.put(SUBJECT_TYPE_CLAIM, subjectType);
        if (role != null) {
            claims.put(ROLE_CLAIM, role);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSecretKey(secret), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析Token
     *
     * @param token Token字符串
     * @return Claims
     */
    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    /**
     * 解析Token
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return Claims
     */
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getSecretKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        return getUserIdFromToken(token, DEFAULT_SECRET);
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 获取主体类型
     */
    public static String getSubjectType(String token) {
        return getSubjectType(token, DEFAULT_SECRET);
    }

    /**
     * 获取主体类型
     */
    public static String getSubjectType(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get(SUBJECT_TYPE_CLAIM, String.class);
    }

    /**
     * 获取角色
     */
    public static String getRole(String token) {
        return getRole(token, DEFAULT_SECRET);
    }

    /**
     * 获取角色
     */
    public static String getRole(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get(ROLE_CLAIM, String.class);
    }

    /**
     * 是否访问 Token
     */
    public static boolean isAccessToken(String token) {
        return isAccessToken(token, DEFAULT_SECRET);
    }

    /**
     * 是否访问 Token
     */
    public static boolean isAccessToken(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    /**
     * 是否刷新 Token
     */
    public static boolean isRefreshToken(String token) {
        return isRefreshToken(token, DEFAULT_SECRET);
    }

    /**
     * 是否刷新 Token
     */
    public static boolean isRefreshToken(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    /**
     * 验证Token是否有效
     *
     * @param token Token字符串
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, DEFAULT_SECRET);
    }

    /**
     * 验证Token是否有效
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 是否有效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误");
            return false;
        } catch (SignatureException e) {
            log.warn("Token签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token为空或非法");
            return false;
        }
    }

    /**
     * 判断Token是否过期
     *
     * @param token Token字符串
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        return isTokenExpired(token, DEFAULT_SECRET);
    }

    /**
     * 判断Token是否过期
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 获取Token过期时间
     *
     * @param token Token字符串
     * @return 过期时间（毫秒）
     */
    public static Long getExpiration(String token) {
        return getExpiration(token, DEFAULT_SECRET);
    }

    /**
     * 获取Token过期时间
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 过期时间（毫秒）
     */
    public static Long getExpiration(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 获取Token剩余有效时间
     *
     * @param token Token字符串
     * @return 剩余有效时间（毫秒）
     */
    public static Long getRemainingTime(String token) {
        return getRemainingTime(token, DEFAULT_SECRET);
    }

    /**
     * 获取Token剩余有效时间
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return 剩余有效时间（毫秒）
     */
    public static Long getRemainingTime(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            long expiration = claims.getExpiration().getTime();
            long now = System.currentTimeMillis();
            return Math.max(0, expiration - now);
        } catch (Exception e) {
            return 0L;
        }
    }
}
