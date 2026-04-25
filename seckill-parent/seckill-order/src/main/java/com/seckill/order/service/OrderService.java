package com.seckill.order.service;

import com.seckill.common.result.PageResult;
import com.seckill.order.dto.OrderDetailResponse;
import com.seckill.order.dto.OrderListResponse;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 查询当前用户的订单列表（分页）
     *
     * @param status  订单状态筛选，null 表示全部
     * @param current 当前页码
     * @param size    每页大小
     * @return 订单列表分页结果
     */
    PageResult<OrderListResponse> listUserOrders(Integer status, Long current, Long size);

    /**
     * 查询订单详情
     *
     * @param orderNo 订单编号
     * @return 订单详情
     */
    OrderDetailResponse getOrderDetail(String orderNo);

    /**
     * 取消订单
     *
     * @param orderNo      订单编号
     * @param cancelReason 取消原因
     */
    void cancelOrder(String orderNo, String cancelReason);
}
