package com.seckill.common.enums;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResponseCodeEnum {

    // 成功响应
    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),

    // 客户端错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 业务错误（40001-49999）
    PARAM_ERROR(40001, "参数错误"),
    USERNAME_EMPTY(40002, "用户名不能为空"),
    PASSWORD_EMPTY(40003, "密码不能为空"),
    USERNAME_FORMAT_ERROR(40004, "用户名格式不正确"),
    PASSWORD_FORMAT_ERROR(40005, "密码格式不正确"),
    PHONE_FORMAT_ERROR(40006, "手机号格式不正确"),
    VERIFY_CODE_ERROR(40007, "验证码错误或已过期"),
    STOCK_NOT_ENOUGH(40008, "商品库存不足"),
    SECKILL_NOT_START(40009, "秒杀活动未开始"),
    SECKILL_ENDED(40010, "秒杀活动已结束"),
    REPEAT_SECKILL(40011, "重复秒杀"),
    ORDER_STATUS_ERROR(40012, "订单状态异常"),
    PAY_AMOUNT_ERROR(40013, "支付金额不正确"),
    ADDRESS_INCOMPLETE(40014, "收货地址信息不完整"),
    FILE_TYPE_NOT_SUPPORT(40015, "文件上传格式不支持"),
    FILE_SIZE_EXCEED(40016, "文件大小超出限制"),
    SECKILL_PATH_INVALID(40017, "秒杀地址无效或已过期"),
    ORDER_ALREADY_PAID(40018, "订单已支付"),
    STOCK_NOT_PREHEATED(40019, "秒杀库存未预热，请稍后再试"),

    // 资源不存在错误（40401-40499）
    USER_NOT_FOUND(40401, "用户不存在"),
    GOODS_NOT_FOUND(40402, "商品不存在"),
    SECKILL_NOT_FOUND(40403, "秒杀活动不存在"),
    ORDER_NOT_FOUND(40404, "订单不存在"),
    ADDRESS_NOT_FOUND(40405, "收货地址不存在"),
    CATEGORY_NOT_FOUND(40406, "分类不存在"),

    // 资源冲突错误（40901-40999）
    USERNAME_EXISTS(40901, "用户名已存在"),
    PHONE_EXISTS(40902, "手机号已注册"),

    // 限流错误（42901-42999）
    RATE_LIMIT(42901, "请求过于频繁，请稍后再试"),
    SECKILL_RATE_LIMIT(42902, "秒杀接口限流，请稍后再试"),

    // 服务器错误
    ERROR(500, "服务器内部错误"),
    REDIS_ERROR(50001, "Redis连接异常"),
    MQ_ERROR(50002, "消息队列异常"),
    DB_ERROR(50003, "数据库操作异常"),
    FILE_UPLOAD_ERROR(50004, "文件上传失败"),
    PAY_SERVICE_ERROR(50005, "支付服务异常");

    private final Integer code;
    private final String message;

    ResponseCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
