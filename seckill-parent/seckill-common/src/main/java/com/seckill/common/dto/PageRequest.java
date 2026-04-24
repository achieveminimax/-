package com.seckill.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求参数
 */
@Data
public class PageRequest {

    /**
     * 当前页码（从1开始）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Long size = 10L;

    /**
     * 获取 MyBatis-Plus 分页对象
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<?> toMpPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
    }

    /**
     * 获取偏移量
     */
    public Long getOffset() {
        return (current - 1) * size;
    }
}
