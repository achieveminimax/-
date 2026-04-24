package com.seckill.application.controller;

import com.seckill.common.ResponseCodeEnum;
import com.seckill.common.Result;
import com.seckill.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于验证项目脚手架是否正常工作
 *
 * @author seckill
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "测试接口", description = "用于验证项目脚手架")
public class TestController {

    /**
     * Hello 测试
     */
    @GetMapping("/hello")
    @Operation(summary = "Hello 测试", description = "返回简单的问候语")
    public String hello() {
        log.info("Hello 接口被调用");
        return "Hello Seckill!";
    }

    /**
     * 统一响应格式测试
     */
    @GetMapping("/result")
    @Operation(summary = "统一响应测试", description = "返回统一响应格式的数据")
    public Result<Map<String, Object>> result() {
        log.info("统一响应测试接口被调用");

        Map<String, Object> data = new HashMap<>();
        data.put("message", "测试成功");
        data.put("timestamp", LocalDateTime.now());
        data.put("version", "1.0.0");

        return Result.success(data);
    }

    /**
     * 异常处理测试
     */
    @GetMapping("/error")
    @Operation(summary = "异常处理测试", description = "抛出业务异常，测试全局异常处理")
    public Result<Void> error() {
        log.info("异常处理测试接口被调用");
        throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "这是一个测试异常");
    }

    /**
     * 系统异常测试
     */
    @GetMapping("/system-error")
    @Operation(summary = "系统异常测试", description = "抛出系统异常，测试全局异常处理")
    public Result<Void> systemError() {
        log.info("系统异常测试接口被调用");
        throw new RuntimeException("这是一个系统异常");
    }

}
