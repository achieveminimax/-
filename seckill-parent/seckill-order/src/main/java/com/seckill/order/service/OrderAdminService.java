package com.seckill.order.service;

/**
 * 管理端订单服务。
 */
public interface OrderAdminService {

    /**
     * 对已支付订单执行发货。
     *
     * @param orderNo 订单号
     * @param expressCompany 快递公司
     * @param expressNo 快递单号
     */
    void shipOrder(String orderNo, String expressCompany, String expressNo);
}
