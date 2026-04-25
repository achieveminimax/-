package com.seckill.admin.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端订单详情响应。
 */
@Data
public class AdminOrderDetailResponse {
    private String orderNo;
    private Long userId;
    private String username;
    private Long activityId;
    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private BigDecimal totalAmount;
    private Integer quantity;
    private Integer status;
    private String statusDesc;
    private Integer payType;
    private LocalDateTime payTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String expressCompany;
    private String expressNo;
    private LocalDateTime shipTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
