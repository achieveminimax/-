package com.seckill.admin.dto.seckill;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端秒杀活动列表响应。
 */
@Data
public class AdminSeckillActivityResponse {

    private Long activityId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusDesc;
    private Integer goodsCount;
    private Integer totalStock;
    private Integer totalSales;
    private LocalDateTime createTime;
}
