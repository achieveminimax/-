package com.seckill.order.dto;

import lombok.Data;

@Data
public class CreateSeckillOrderCommand {
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

    /**
     * 获取秒杀记录ID（别名方法）
     */
    public Long getSeckillRecordId() {
        return recordId;
    }
}
