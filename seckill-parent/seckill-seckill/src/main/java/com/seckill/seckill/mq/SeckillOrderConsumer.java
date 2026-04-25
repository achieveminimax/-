package com.seckill.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.order.dto.CreateSeckillOrderCommand;
import com.seckill.order.dto.CreateSeckillOrderResult;
import com.seckill.order.service.OrderCreateService;
import com.seckill.seckill.entity.SeckillRecord;
import com.seckill.seckill.mapper.SeckillRecordMapper;
import com.seckill.seckill.service.SeckillPathService;
import com.seckill.seckill.service.SeckillRecordService;
import com.seckill.seckill.service.StockService;
import com.seckill.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final int MAX_RETRY_COUNT = 2;

    private final OrderCreateService orderCreateService;
    private final SeckillRecordService seckillRecordService;
    private final SeckillRecordMapper seckillRecordMapper;
    private final GoodsMapper goodsMapper;
    private final SeckillPathService seckillPathService;
    private final StockService stockService;
    private final SeckillOrderProducer seckillOrderProducer;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void consume(SeckillOrderMessage payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        SeckillRecord record = seckillRecordMapper.selectById(payload.getRecordId());
        if (record == null
                || record.getStatus().equals(SeckillRecordStatusEnum.SUCCESS.getCode())
                || record.getStatus().equals(SeckillRecordStatusEnum.FAILED.getCode())) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        Goods goods = null;
        SeckillGoods seckillGoods = null;

        try {
            goods = goodsMapper.selectById(payload.getGoodsId());
            seckillGoods = seckillPathService.requireSeckillGoods(payload.getActivityId(), payload.getGoodsId());

            CreateSeckillOrderCommand command = new CreateSeckillOrderCommand();
            command.setRecordId(payload.getRecordId());
            command.setOrderNo(payload.getOrderNo());
            command.setUserId(payload.getUserId());
            command.setActivityId(payload.getActivityId());
            command.setGoodsId(payload.getGoodsId());
            command.setQuantity(payload.getQuantity());
            command.setAddressId(payload.getAddressId());
            command.setReceiverName(payload.getReceiverName());
            command.setReceiverPhone(payload.getReceiverPhone());
            command.setReceiverAddress(payload.getReceiverAddress());

            CreateSeckillOrderResult result = orderCreateService.createSeckillOrder(command);
            seckillRecordService.markSuccess(payload.getRecordId(), result.getOrderId(), result.getOrderNo(), goods, seckillGoods);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            int retryCount = resolveRetryCount(message);
            log.error("处理秒杀订单失败, recordId={}, retryCount={}", payload.getRecordId(), retryCount, ex);

            if (retryCount < MAX_RETRY_COUNT && shouldRetry(ex)) {
                retryOrRequeue(payload, deliveryTag, retryCount, channel);
                return;
            }

            if (goods == null) {
                goods = goodsMapper.selectById(payload.getGoodsId());
            }
            if (seckillGoods == null) {
                seckillGoods = loadSeckillGoods(payload.getActivityId(), payload.getGoodsId());
            }
            if (!record.getStatus().equals(SeckillRecordStatusEnum.FAILED.getCode())) {
                seckillRecordService.markFailed(payload.getRecordId(), buildFailureReason(ex), goods, seckillGoods);
                stockService.rollback(payload.getActivityId(), payload.getGoodsId(), payload.getUserId(), payload.getQuantity() == null ? 1 : payload.getQuantity());
            }
            channel.basicReject(deliveryTag, false);
        }
    }

    private void retryOrRequeue(SeckillOrderMessage payload, long deliveryTag, int retryCount, Channel channel) throws IOException {
        try {
            seckillOrderProducer.send(payload, retryCount + 1);
            channel.basicAck(deliveryTag, false);
        } catch (Exception retryEx) {
            log.error("重投秒杀消息失败, recordId={}, retryCount={}", payload.getRecordId(), retryCount + 1, retryEx);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private int resolveRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get(SeckillOrderProducer.RETRY_COUNT_HEADER);
        if (retryCount instanceof Number number) {
            return number.intValue();
        }
        if (retryCount instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean shouldRetry(Exception ex) {
        return !(ex instanceof BusinessException businessException
                && businessException.getCode() != null
                && businessException.getCode() >= 40000
                && businessException.getCode() < 50000);
    }

    private String buildFailureReason(Exception ex) {
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? "秒杀下单失败" : message;
    }

    private SeckillGoods loadSeckillGoods(Long activityId, Long goodsId) {
        try {
            return seckillPathService.requireSeckillGoods(activityId, goodsId);
        } catch (Exception ex) {
            log.warn("加载秒杀商品失败, activityId={}, goodsId={}", activityId, goodsId, ex);
            return null;
        }
    }
}
