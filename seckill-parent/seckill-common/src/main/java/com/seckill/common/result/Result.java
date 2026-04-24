package com.seckill.common.result;

import com.seckill.common.enums.ResponseCodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), message, data);
    }

    /**
     * 创建成功响应
     */
    public static <T> Result<T> created(T data) {
        return new Result<>(ResponseCodeEnum.CREATED.getCode(), ResponseCodeEnum.CREATED.getMessage(), data);
    }

    /**
     * 错误响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResponseCodeEnum.ERROR.getCode(), message, null);
    }

    /**
     * 错误响应（带状态码）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 错误响应（使用枚举）
     */
    public static <T> Result<T> error(ResponseCodeEnum responseCodeEnum) {
        return new Result<>(responseCodeEnum.getCode(), responseCodeEnum.getMessage(), null);
    }

    /**
     * 错误响应（使用枚举，自定义消息）
     */
    public static <T> Result<T> error(ResponseCodeEnum responseCodeEnum, String message) {
        return new Result<>(responseCodeEnum.getCode(), message, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResponseCodeEnum.SUCCESS.getCode().equals(this.code);
    }
}
