package com.seckill.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.order.dto.CreateSeckillOrderCommand;
import com.seckill.order.dto.CreateSeckillOrderResult;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.mq.OrderTimeoutMessage;
import com.seckill.order.mq.OrderTimeoutProducer;
import com.seckill.order.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateServiceImpl implements OrderCreateService {

    private final OrderMapper orderMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final OrderTimeoutProducer orderTimeoutProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateSeckillOrderResult createSeckillOrder(CreateSeckillOrderCommand command) {
        Order existed = orderMapper.selectByOrderNo(command.getOrderNo());
        if (existed != null) {
            return new CreateSeckillOrderResult(existed.getId(), existed.getOrderNo());
        }

        SeckillActivity activity = seckillActivityMapper.selectById(command.getActivityId());
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }

        Goods goods = goodsMapper.selectById(command.getGoodsId());
        if (goods == null || (goods.getDeleted() != null && goods.getDeleted() == 1)) {
            throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
        }

        SeckillGoods seckillGoods = seckillGoodsMapper.selectOne(new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, command.getActivityId())
                .eq(SeckillGoods::getGoodsId, command.getGoodsId())
                .last("LIMIT 1"));
        if (seckillGoods == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND, "秒杀商品不存在");
        }

        int quantity = command.getQuantity() == null || command.getQuantity() < 1 ? 1 : command.getQuantity();
        if (goodsMapper.deductStock(command.getGoodsId(), quantity) < 1) {
            throw new BusinessException(ResponseCodeEnum.STOCK_NOT_ENOUGH);
        }
        if (seckillGoodsMapper.incrementSalesCount(command.getActivityId(), command.getGoodsId(), quantity) < 1) {
            throw new BusinessException(ResponseCodeEnum.STOCK_NOT_ENOUGH);
        }

        Order order = new Order();
        order.setOrderNo(command.getOrderNo());
        order.setUserId(command.getUserId());
        order.setActivityId(command.getActivityId());
        order.setGoodsId(command.getGoodsId());
        order.setGoodsName(goods.getName());
        order.setGoodsImage(goods.getCoverImage());
        order.setOrderPrice(seckillGoods.getSeckillPrice());
        order.setQuantity(quantity);
        order.setStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setReceiverName(command.getReceiverName());
        order.setReceiverPhone(command.getReceiverPhone());
        order.setReceiverAddress(command.getReceiverAddress());
        order.setAddressId(command.getAddressId());
        order.setDeleted(0);
        orderMapper.insert(order);

        // 发送订单超时延迟消息
        sendOrderTimeoutMessage(order, command.getSeckillRecordId());

        return new CreateSeckillOrderResult(order.getId(), order.getOrderNo());
    }

    /**
     * 发送订单超时延迟消息
     */
    private void sendOrderTimeoutMessage(Order order, Long seckillRecordId) {
        try {
            OrderTimeoutMessage message = new OrderTimeoutMessage();
            message.setOrderNo(order.getOrderNo());
            message.setUserId(order.getUserId());
            message.setActivityId(order.getActivityId());
            message.setGoodsId(order.getGoodsId());
            message.setQuantity(order.getQuantity());
            message.setSeckillRecordId(seckillRecordId);
            orderTimeoutProducer.send(message);
        } catch (Exception e) {
            log.error("发送订单超时延迟消息失败, orderNo={}", order.getOrderNo(), e);
            // 不影响主流程，继续返回订单结果
        }
    }
}
