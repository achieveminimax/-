package com.seckill.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品上下架状态更新请求体。
 */
@Data
public class GoodsStatusUpdateRequest {
    /** 商品状态：0-下架，1-上架。 */
    @NotNull(message = "商品状态不能为空")
    @Min(value = 0, message = "商品状态非法")
    @Max(value = 1, message = "商品状态非法")
    private Integer status;
}
