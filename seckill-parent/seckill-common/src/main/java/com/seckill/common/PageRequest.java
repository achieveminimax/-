package com.seckill.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求参数封装
 *
 * @author seckill
 */
@Data
public class PageRequest {

    /**
     * 当前页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;

    /**
     * 获取 MyBatis-Plus 的当前页（从0开始）
     */
    public long getCurrent() {
        return pageNum - 1;
    }

    /**
     * 获取偏移量
     */
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

}
