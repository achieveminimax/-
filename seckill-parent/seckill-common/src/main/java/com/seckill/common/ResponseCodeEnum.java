package com.seckill.common;

import lombok.Getter;

/**
 * 响应码枚举
 * 统一规范 API 响应状态码
 *
 * @author seckill
 */
@Getter
public enum ResponseCodeEnum {

    // ==================== 成功状态码 ====================
    SUCCESS(200, "操作成功"),

    // ==================== 客户端错误状态码 (400xx) ====================
    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(40001, "缺少必要参数"),
    PARAM_INVALID(40002, "参数格式不正确"),
    PARAM_TYPE_ERROR(40003, "参数类型错误"),

    // 认证相关 (401xx)
    UNAUTHORIZED(401, "未登录或登录已过期"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),

    // 权限相关 (403xx)
    FORBIDDEN(403, "无权限访问"),
    PERMISSION_DENIED(40301, "权限不足"),

    // 资源相关 (404xx)
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    GOODS_NOT_FOUND(40402, "商品不存在"),
    ORDER_NOT_FOUND(40403, "订单不存在"),
    ACTIVITY_NOT_FOUND(40404, "活动不存在"),

    // 请求频率限制 (429xx)
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    RATE_LIMIT_EXCEEDED(42901, "超出限流阈值"),

    // ==================== 业务错误状态码 (400xx-499xx) ====================
    // 用户相关 (410xx)
    USER_EXISTS(41001, "用户已存在"),
    USERNAME_EXISTS(41002, "用户名已被注册"),
    PHONE_EXISTS(41003, "手机号已被注册"),
    USER_DISABLED(41004, "用户已被禁用"),
    USER_LOCKED(41005, "账号已被锁定"),
    LOGIN_FAIL(41006, "登录失败，用户名或密码错误"),
    PASSWORD_ERROR(41007, "密码错误"),

    // 商品相关 (420xx)
    GOODS_OFF_SHELF(42001, "商品已下架"),
    GOODS_STOCK_EMPTY(42002, "商品库存不足"),
    GOODS_PRICE_CHANGED(42003, "商品价格已变动"),

    // 秒杀相关 (430xx)
    SECKILL_NOT_START(43001, "秒杀活动未开始"),
    SECKILL_ALREADY_END(43002, "秒杀活动已结束"),
    SECKILL_STOCK_EMPTY(43003, "秒杀商品已售罄"),
    SECKILL_LIMIT_EXCEED(43004, "超出每人限购数量"),
    SECKILL_REPEAT(43005, "您已参与过该商品的秒杀"),
    SECKILL_PATH_ERROR(43006, "秒杀地址错误"),
    SECKILL_VERIFY_FAIL(43007, "验证码错误或已过期"),

    // 订单相关 (440xx)
    ORDER_CREATE_FAIL(44001, "订单创建失败"),
    ORDER_STATUS_ERROR(44002, "订单状态不正确"),
    ORDER_PAY_TIMEOUT(44003, "订单支付超时"),
    ORDER_ALREADY_PAID(44004, "订单已支付"),
    ORDER_CANCEL_FAIL(44005, "订单取消失败"),
    ORDER_PAY_FAIL(44006, "订单支付失败"),

    // 支付相关 (450xx)
    PAY_AMOUNT_ERROR(45001, "支付金额不正确"),
    PAY_METHOD_NOT_SUPPORT(45002, "不支持的支付方式"),
    PAY_BALANCE_INSUFFICIENT(45003, "余额不足"),

    // ==================== 服务器错误状态码 (500xx) ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    DATABASE_ERROR(50001, "数据库操作失败"),
    REDIS_ERROR(50002, "Redis 操作失败"),
    MQ_ERROR(50003, "消息队列操作失败"),
    EXTERNAL_SERVICE_ERROR(50004, "外部服务调用失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态信息
     */
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
