package com.seckill.seckill.dto;

import lombok.Data;

@Data
public class SeckillExecuteResponse {
    private Long recordId;
    private Integer status;
    private String statusDesc;
    private String message;
}
