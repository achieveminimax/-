package com.seckill.order.service;

import com.seckill.order.dto.PayCreateResponse;
import com.seckill.order.dto.PayStatusResponse;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 创建支付
     *
     * @param orderNo 订单编号
     * @param payType 支付方式
     * @return 支付创建响应
     */
    PayCreateResponse createPay(String orderNo, Integer payType);

    /**
     * 支付回调
     *
     * @param payNo   支付流水号
     * @param tradeNo 第三方交易号
     */
    void payCallback(String payNo, String tradeNo);

    /**
     * 查询支付状态
     *
     * @param orderNo 订单编号
     * @return 支付状态响应
     */
    PayStatusResponse queryPayStatus(String orderNo);
}
