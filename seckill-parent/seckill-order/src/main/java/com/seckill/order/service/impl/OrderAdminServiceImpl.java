package com.seckill.order.service.impl;

import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.service.OrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 管理端订单服务实现。
 */
@Service
@RequiredArgsConstructor
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(String orderNo, String expressCompany, String expressNo) {
        if (!StringUtils.hasText(expressCompany) || !StringUtils.hasText(expressNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "快递公司和快递单号不能为空");
        }

        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResponseCodeEnum.ORDER_NOT_FOUND);
        }
        if (!OrderStatusEnum.allowShip(order.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.ORDER_STATUS_ERROR);
        }

        order.setExpressCompany(expressCompany.trim());
        order.setExpressNo(expressNo.trim());
        order.setShipTime(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.SHIPPED.getCode());
        orderMapper.updateById(order);
    }
}
