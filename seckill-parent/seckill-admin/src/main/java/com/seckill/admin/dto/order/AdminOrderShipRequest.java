package com.seckill.admin.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端订单发货请求。
 */
@Data
public class AdminOrderShipRequest {

    @NotBlank(message = "快递公司不能为空")
    @Size(min = 2, max = 50, message = "快递公司长度必须在2-50位之间")
    private String expressCompany;

    @NotBlank(message = "快递单号不能为空")
    @Size(min = 5, max = 30, message = "快递单号长度必须在5-30位之间")
    private String expressNo;
}
