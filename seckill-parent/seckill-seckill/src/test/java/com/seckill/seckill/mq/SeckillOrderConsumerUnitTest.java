package com.seckill.seckill.mq;

import com.rabbitmq.client.Channel;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.order.dto.CreateSeckillOrderResult;
import com.seckill.order.service.OrderCreateService;
import com.seckill.seckill.entity.SeckillRecord;
import com.seckill.seckill.mapper.SeckillRecordMapper;
import com.seckill.seckill.service.SeckillPathService;
import com.seckill.seckill.service.SeckillRecordService;
import com.seckill.seckill.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillOrderConsumer 单元测试")
class SeckillOrderConsumerUnitTest {

    @Mock
    private OrderCreateService orderCreateService;

    @Mock
    private SeckillRecordService seckillRecordService;

    @Mock
    private SeckillRecordMapper seckillRecordMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private SeckillPathService seckillPathService;

    @Mock
    private StockService stockService;

    @Mock
    private SeckillOrderProducer seckillOrderProducer;

    @Mock
    private Channel channel;

    @Test
    @DisplayName("消费成功 - 创建订单并 ACK")
    void consume_Success_ShouldAck() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        Message message = messageWithRetryCount(0);
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenReturn(new CreateSeckillOrderResult(7001L, "ORD-1"));

        consumer.consume(payload, message, channel);

        verify(seckillRecordService).markSuccess(9001L, 7001L, "ORD-1", goods, seckillGoods);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("消费幂等 - 已成功记录直接 ACK")
    void consume_Idempotent_WhenRecordAlreadySuccess() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillRecord record = record(SeckillRecordStatusEnum.SUCCESS);

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);

        consumer.consume(payload(), messageWithRetryCount(0), channel);

        verify(channel).basicAck(1L, false);
        verify(orderCreateService, never()).createSeckillOrder(any());
    }

    @Test
    @DisplayName("消费幂等 - 记录不存在直接 ACK")
    void consume_Idempotent_WhenRecordNull() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(null);

        consumer.consume(payload(), messageWithRetryCount(0), channel);

        verify(channel).basicAck(1L, false);
        verify(orderCreateService, never()).createSeckillOrder(any());
    }

    @Test
    @DisplayName("消费幂等 - 已失败记录直接 ACK")
    void consume_Idempotent_WhenRecordAlreadyFailed() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillRecord record = record(SeckillRecordStatusEnum.FAILED);

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);

        consumer.consume(payload(), messageWithRetryCount(0), channel);

        verify(channel).basicAck(1L, false);
        verify(orderCreateService, never()).createSeckillOrder(any());
    }

    @Test
    @DisplayName("消费失败可重试 - 重投主队列并 ACK 当前消息")
    void consume_Retry_WhenTransientErrorAndRetryCountBelowLimit() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("db down"));

        consumer.consume(payload, messageWithRetryCount(1), channel);

        verify(seckillOrderProducer).send(payload, 2);
        verify(channel).basicAck(1L, false);
        verify(seckillRecordService, never()).markFailed(anyLong(), any(), any(), any());
        verify(stockService, never()).rollback(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("消费失败超限 - 标记失败、回滚库存并拒绝进入死信")
    void consume_FinalFail_WhenRetryCountReachedLimit() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("db down"));

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(seckillRecordService).markFailed(9001L, "db down", goods, seckillGoods);
        verify(stockService).rollback(1L, 2001L, 1001L, 1);
        verify(channel).basicReject(1L, false);
    }

    @Test
    @DisplayName("重投失败 - 回退为 NACK 原消息重新入队")
    void consume_Requeue_WhenRetryPublishFails() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("db down"));
        doThrow(new RuntimeException("republish failed")).when(seckillOrderProducer).send(payload, 2);

        consumer.consume(payload, messageWithRetryCount(1), channel);

        verify(channel).basicNack(1L, false, true);
        verify(seckillRecordService, never()).markFailed(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("业务异常(4xx)不重试 - 直接标记失败并拒绝消息")
    void consume_BusinessExceptionNoRetry_FinalFail() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any()))
                .thenThrow(new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND));

        consumer.consume(payload, messageWithRetryCount(0), channel);

        verify(seckillRecordService).markFailed(eq(9001L), anyString(), eq(goods), eq(seckillGoods));
        verify(stockService).rollback(1L, 2001L, 1001L, 1);
        verify(channel).basicReject(1L, false);
        verify(seckillOrderProducer, never()).send(any(), anyInt());
    }

    @Test
    @DisplayName("重试次数从String类型Header解析")
    void consume_RetryCountAsString_Success() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("transient error"));

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        properties.setHeader(SeckillOrderProducer.RETRY_COUNT_HEADER, "1");
        Message message = new Message(new byte[0], properties);

        consumer.consume(payload, message, channel);

        verify(seckillOrderProducer).send(payload, 2);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("重试次数Header为null时默认0")
    void consume_RetryCountNull_DefaultZero() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("transient error"));

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        Message message = new Message(new byte[0], properties);

        consumer.consume(payload, message, channel);

        verify(seckillOrderProducer).send(payload, 1);
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("异常信息为null时使用默认失败原因")
    void consume_ExceptionNullMessage_DefaultReason() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException((String) null));

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(seckillRecordService).markFailed(eq(9001L), eq("秒杀下单失败"), eq(goods), eq(seckillGoods));
        verify(channel).basicReject(1L, false);
    }

    @Test
    @DisplayName("异常信息为空白时使用默认失败原因")
    void consume_ExceptionBlankMessage_DefaultReason() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("   "));

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(seckillRecordService).markFailed(eq(9001L), eq("秒杀下单失败"), eq(goods), eq(seckillGoods));
        verify(channel).basicReject(1L, false);
    }

    @Test
    @DisplayName("quantity为null时回滚库存默认使用1")
    void consume_QuantityNull_DefaultsToOne() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        payload.setQuantity(null);
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        when(orderCreateService.createSeckillOrder(any())).thenThrow(new RuntimeException("fail"));

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(stockService).rollback(1L, 2001L, 1001L, 1);
    }

    @Test
    @DisplayName("catch块中加载秒杀商品失败时使用null")
    void consume_LoadSeckillGoodsFails_UseNull() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L))
                .thenThrow(new RuntimeException("not found"));

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(seckillRecordService).markFailed(eq(9001L), anyString(), eq(goods), eq(null));
        verify(channel).basicReject(1L, false);
    }

    @Test
    @DisplayName("catch块中goods为null时重新查询商品")
    void consume_GoodsNullInCatch_ReloadGoods() throws IOException {
        SeckillOrderConsumer consumer = buildConsumer();
        SeckillOrderMessage payload = payload();
        SeckillRecord record = record(SeckillRecordStatusEnum.QUEUING);
        Goods goods = goods();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillRecordMapper.selectById(9001L)).thenReturn(record);
        when(goodsMapper.selectById(2001L))
                .thenThrow(new RuntimeException("first call fail"))
                .thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);

        consumer.consume(payload, messageWithRetryCount(2), channel);

        verify(seckillRecordService).markFailed(eq(9001L), anyString(), eq(goods), eq(seckillGoods));
    }

    private SeckillOrderConsumer buildConsumer() {
        return new SeckillOrderConsumer(
                orderCreateService,
                seckillRecordService,
                seckillRecordMapper,
                goodsMapper,
                seckillPathService,
                stockService,
                seckillOrderProducer
        );
    }

    private SeckillOrderMessage payload() {
        SeckillOrderMessage payload = new SeckillOrderMessage();
        payload.setRecordId(9001L);
        payload.setOrderNo("ORD-1");
        payload.setUserId(1001L);
        payload.setActivityId(1L);
        payload.setGoodsId(2001L);
        payload.setQuantity(1);
        payload.setAddressId(3001L);
        payload.setReceiverName("张三");
        payload.setReceiverPhone("13800138000");
        payload.setReceiverAddress("深圳市南山区科技园");
        return payload;
    }

    private Message messageWithRetryCount(int retryCount) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(1L);
        properties.setHeader(SeckillOrderProducer.RETRY_COUNT_HEADER, retryCount);
        return new Message(new byte[0], properties);
    }

    private SeckillRecord record(SeckillRecordStatusEnum status) {
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);
        record.setStatus(status.getCode());
        return record;
    }

    private Goods goods() {
        Goods goods = new Goods();
        goods.setId(2001L);
        goods.setName("测试商品");
        return goods;
    }

    private SeckillGoods seckillGoods() {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(1L);
        seckillGoods.setGoodsId(2001L);
        seckillGoods.setSeckillPrice(new BigDecimal("99.00"));
        return seckillGoods;
    }
}
