package com.seckill.order.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单超时消息
 * 用于延迟队列，订单创建后发送，TTL到期后触发取消逻辑
 */
@Data
public class OrderTimeoutMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 秒杀记录ID
     */
    private Long seckillRecordId;

    /**
     * 消息创建时间
     */
    private Long messageCreateTime;

    public OrderTimeoutMessage() {
        this.messageCreateTime = System.currentTimeMillis();
    }
}
