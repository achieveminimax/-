package com.seckill.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应结果封装
 * 所有接口返回的数据都包装在此类中
 *
 * @param <T> 数据类型
 * @author seckill
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 提示信息
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

    // ==================== 成功响应 ====================

    /**
     * 成功响应（无数据）
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

    // ==================== 失败响应 ====================

    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(ResponseCodeEnum.INTERNAL_ERROR.getCode(), ResponseCodeEnum.INTERNAL_ERROR.getMessage(), null);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResponseCodeEnum.INTERNAL_ERROR.getCode(), message, null);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（使用响应码枚举）
     */
    public static <T> Result<T> error(ResponseCodeEnum responseCode) {
        return new Result<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    /**
     * 失败响应（使用响应码枚举，自定义消息）
     */
    public static <T> Result<T> error(ResponseCodeEnum responseCode, String message) {
        return new Result<>(responseCode.getCode(), message, null);
    }

    // ==================== 判断方法 ====================

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code.equals(ResponseCodeEnum.SUCCESS.getCode());
    }

    /**
     * 是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }

}
