package com.seckill.order.service;

/**
 * 库存回滚服务接口
 * 用于订单取消或超时后的库存回滚
 */
public interface StockRollbackService {

    /**
     * 回滚库存
     * 包括：Redis库存、数据库库存、秒杀销量、用户已秒杀标记
     *
     * @param activityId 活动ID
     * @param goodsId    商品ID
     * @param userId     用户ID
     * @param quantity   回滚数量
     */
    void rollbackStock(Long activityId, Long goodsId, Long userId, int quantity);
}
