package com.seckill.admin.dto.seckill;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端秒杀活动商品项请求。
 */
@Data
public class AdminSeckillGoodsItemRequest {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价格必须大于0")
    private BigDecimal seckillPrice;

    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存必须大于0")
    private Integer seckillStock;

    @Min(value = 1, message = "限购数量至少为1")
    private Integer limitPerUser = 1;
}
