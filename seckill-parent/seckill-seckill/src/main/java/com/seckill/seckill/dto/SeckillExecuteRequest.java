package com.seckill.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillExecuteRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotBlank(message = "秒杀地址不能为空")
    private String seckillPath;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity = 1;
}
