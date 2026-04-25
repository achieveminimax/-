package com.seckill.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.common.utils.UserContext;
import com.seckill.order.dto.OrderDetailResponse;
import com.seckill.order.dto.OrderListResponse;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.OrderService;
import com.seckill.order.service.StockRollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final StockRollbackService stockRollbackService;

    /**
     * 订单超时时间（分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    @Override
    public PageResult<OrderListResponse> listUserOrders(Integer status, Long current, Long size) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED);
        }

        // 设置分页参数
        long pageNum = (current == null || current < 1) ? 1 : current;
        long pageSize = (size == null || size < 1) ? 10 : size;
        if (pageSize > 100) {
            pageSize = 100;
        }

        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getDeleted, 0);

        // 状态筛选
        if (status != null && status > 0) {
            wrapper.eq(Order::getStatus, status);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> resultPage = orderMapper.selectPage(page, wrapper);

        List<OrderListResponse> records = resultPage.getRecords().stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }

    @Override
    public OrderDetailResponse getOrderDetail(String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED);
        }

        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "订单编号不能为空");
        }

        Order order = orderMapper.selectOwnedOrder(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ResponseCodeEnum.ORDER_NOT_FOUND);
        }

        return convertToDetailResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, String cancelReason) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED);
        }

        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "订单编号不能为空");
        }

        // 查询订单
        Order order = orderMapper.selectOwnedOrder(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ResponseCodeEnum.ORDER_NOT_FOUND);
        }

        // 校验订单状态是否允许取消
        if (!OrderStatusEnum.allowCancel(order.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
        }

        // 执行库存回滚
        try {
            stockRollbackService.rollbackStock(
                    order.getActivityId(),
                    order.getGoodsId(),
                    userId,
                    order.getQuantity() == null ? 1 : order.getQuantity()
            );
            log.info("订单取消库存回滚成功, orderNo={}, userId={}, activityId={}, goodsId={}",
                    orderNo, userId, order.getActivityId(), order.getGoodsId());
        } catch (Exception e) {
            log.error("订单取消库存回滚失败, orderNo={}, userId={}", orderNo, userId, e);
            throw new BusinessException(ResponseCodeEnum.ERROR, "库存回滚失败，请稍后重试");
        }

        // 更新订单状态为已取消
        int affected = orderMapper.cancelOrder(
                orderNo,
                userId,
                OrderStatusEnum.PENDING_PAY.getCode(),
                OrderStatusEnum.CANCELLED.getCode(),
                StringUtils.hasText(cancelReason) ? cancelReason : "用户主动取消"
        );

        if (affected < 1) {
            throw new BusinessException(ResponseCodeEnum.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }

        log.info("订单取消成功, orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 转换为列表响应对象
     */
    private OrderListResponse convertToListResponse(Order order) {
        OrderListResponse response = new OrderListResponse();
        response.setOrderNo(order.getOrderNo());
        response.setGoodsId(order.getGoodsId());
        response.setGoodsName(order.getGoodsName());
        response.setGoodsImage(order.getGoodsImage());
        response.setOrderPrice(order.getOrderPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getOrderPrice() != null && order.getQuantity() != null
                ? order.getOrderPrice().multiply(new java.math.BigDecimal(order.getQuantity()))
                : order.getOrderPrice());
        response.setStatus(order.getStatus());

        OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(order.getStatus());
        response.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");
        response.setCreateTime(order.getCreateTime());

        // 计算支付截止时间（仅待支付订单）
        if (OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus()) && order.getCreateTime() != null) {
            response.setPayDeadline(order.getCreateTime().plusMinutes(ORDER_TIMEOUT_MINUTES));
        }

        return response;
    }

    /**
     * 转换为详情响应对象
     */
    private OrderDetailResponse convertToDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setActivityId(order.getActivityId());
        response.setGoodsId(order.getGoodsId());
        response.setGoodsName(order.getGoodsName());
        response.setGoodsImage(order.getGoodsImage());
        response.setOrderPrice(order.getOrderPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getOrderPrice() != null && order.getQuantity() != null
                ? order.getOrderPrice().multiply(new java.math.BigDecimal(order.getQuantity()))
                : order.getOrderPrice());
        response.setPayAmount(order.getOrderPrice());
        response.setStatus(order.getStatus());

        OrderStatusEnum statusEnum = OrderStatusEnum.getByCode(order.getStatus());
        response.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");

        response.setPayType(order.getPayType());
        response.setPayTime(order.getPayTime());

        // 设置收货地址快照
        OrderDetailResponse.AddressSnapshot snapshot = new OrderDetailResponse.AddressSnapshot();
        snapshot.setReceiverName(order.getReceiverName());
        snapshot.setReceiverPhone(order.getReceiverPhone());

        // 解析收货地址
        String fullAddress = order.getReceiverAddress();
        if (StringUtils.hasText(fullAddress)) {
            String[] parts = fullAddress.split("\\|");
            if (parts.length >= 4) {
                snapshot.setProvince(parts[0]);
                snapshot.setCity(parts[1]);
                snapshot.setDistrict(parts[2]);
                snapshot.setDetailAddress(parts[3]);
            } else {
                snapshot.setDetailAddress(fullAddress);
            }
        }
        response.setAddressSnapshot(snapshot);

        response.setExpressCompany(order.getExpressCompany());
        response.setExpressNo(order.getExpressNo());
        response.setCreateTime(order.getCreateTime());
        response.setUpdateTime(order.getUpdateTime());
        response.setCancelReason(order.getCancelReason());

        // 计算支付截止时间（仅待支付订单）
        if (OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus()) && order.getCreateTime() != null) {
            response.setPayDeadline(order.getCreateTime().plusMinutes(ORDER_TIMEOUT_MINUTES));
        }

        return response;
    }
}
