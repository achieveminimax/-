package com.seckill.seckill.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class SeckillOrderMessage implements Serializable {
    private Long recordId;
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long goodsId;
    private Integer quantity;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
