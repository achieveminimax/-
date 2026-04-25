package com.seckill.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillResultResponse {
    private Long recordId;
    private Long activityId;
    private Long goodsId;
    private String goodsName;
    private BigDecimal seckillPrice;
    private Integer status;
    private String statusDesc;
    private String orderNo;
    private String failReason;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
