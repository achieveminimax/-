package com.seckill.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateSeckillOrderResult {
    private Long orderId;
    private String orderNo;
}
