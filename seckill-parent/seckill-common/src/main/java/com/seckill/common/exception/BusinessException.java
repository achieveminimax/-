package com.seckill.common.exception;

import com.seckill.common.ResponseCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 * 用于抛出业务逻辑相关的异常
 *
 * @author seckill
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造方法（使用响应码枚举）
     */
    public BusinessException(ResponseCodeEnum responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    /**
     * 构造方法（使用响应码枚举，自定义消息）
     */
    public BusinessException(ResponseCodeEnum responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }

    /**
     * 构造方法（自定义错误码和消息）
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
