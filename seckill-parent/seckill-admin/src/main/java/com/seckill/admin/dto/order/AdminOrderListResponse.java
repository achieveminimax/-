package com.seckill.admin.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端订单列表响应。
 */
@Data
public class AdminOrderListResponse {
    private String orderNo;
    private Long userId;
    private String username;
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
