package com.seckill.order.service.impl;

import com.seckill.common.enums.OrderStatusEnum;
import com.seckill.common.enums.PayStatusEnum;
import com.seckill.common.enums.PayTypeEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.SnowflakeIdWorker;
import com.seckill.common.utils.UserContext;
import com.seckill.infrastructure.config.RabbitMQConfig;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.dto.PayCreateResponse;
import com.seckill.order.dto.PayStatusResponse;
import com.seckill.order.entity.Order;
import com.seckill.order.entity.PayRecord;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.order.mapper.PayRecordMapper;
import com.seckill.order.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    private final OrderMapper orderMapper;
    private final PayRecordMapper payRecordMapper;
    private final RedisOperations redisOperations;
    private final RabbitOperations rabbitOperations;
    private final SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    public PayServiceImpl(OrderMapper orderMapper,
                          PayRecordMapper payRecordMapper,
                          RedisOperations redisOperations,
                          RabbitOperations rabbitOperations,
                          SnowflakeIdWorker snowflakeIdWorker) {
        this.orderMapper = orderMapper;
        this.payRecordMapper = payRecordMapper;
        this.redisOperations = redisOperations;
        this.rabbitOperations = rabbitOperations;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    /**
     * 支付状态缓存前缀
     */
    private static final String PAY_STATUS_CACHE_PREFIX = "seckill:pay:status:";

    /**
     * 支付状态缓存时间（分钟）
     */
    private static final long PAY_STATUS_CACHE_MINUTES = 5;

    /**
     * 订单超时时间（分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayCreateResponse createPay(String orderNo, Integer payType) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED);
        }

        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "订单编号不能为空");
        }

        // 校验支付方式
        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
        if (payTypeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "支付方式不正确");
        }

        // 查询订单
        Order order = orderMapper.selectOwnedOrder(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ResponseCodeEnum.ORDER_NOT_FOUND);
        }

        // 校验订单状态
        if (!OrderStatusEnum.allowPay(order.getStatus())) {
            if (OrderStatusEnum.PAID.getCode().equals(order.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.ORDER_ALREADY_PAID);
            }
            throw new BusinessException(ResponseCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许支付");
        }

        // 检查是否已存在待支付记录
        PayRecord existRecord = payRecordMapper.selectByOrderNo(orderNo);
        if (existRecord != null && PayStatusEnum.PENDING.getCode().equals(existRecord.getStatus())) {
            // 返回已有的支付记录
            return buildPayCreateResponse(existRecord, order);
        }

        // 创建支付记录
        PayRecord payRecord = new PayRecord();
        payRecord.setPayNo(generatePayNo());
        payRecord.setOrderNo(orderNo);
        payRecord.setPayMethod(payType);
        payRecord.setPayAmount(order.getOrderPrice());
        payRecord.setStatus(PayStatusEnum.PENDING.getCode());
        payRecord.setCreateTime(LocalDateTime.now());
        payRecord.setUpdateTime(LocalDateTime.now());
        payRecordMapper.insert(payRecord);

        log.info("创建支付记录成功, payNo={}, orderNo={}, userId={}, amount={}",
                payRecord.getPayNo(), orderNo, userId, payRecord.getPayAmount());

        return buildPayCreateResponse(payRecord, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(String payNo, String tradeNo) {
        if (!StringUtils.hasText(payNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "支付流水号不能为空");
        }

        // 查询支付记录
        PayRecord payRecord = payRecordMapper.selectByPayNo(payNo);
        if (payRecord == null) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND, "支付记录不存在");
        }

        // 幂等性校验：已支付的直接返回
        if (PayStatusEnum.PAID.getCode().equals(payRecord.getStatus())) {
            log.info("支付记录已处理, payNo={}", payNo);
            return;
        }

        // 校验支付状态
        if (!PayStatusEnum.PENDING.getCode().equals(payRecord.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.ORDER_STATUS_ERROR, "支付状态异常");
        }

        String orderNo = payRecord.getOrderNo();

        // 更新支付记录状态
        LocalDateTime payTime = LocalDateTime.now();
        int affected = payRecordMapper.updateStatus(
                payNo,
                PayStatusEnum.PAID.getCode(),
                tradeNo,
                payTime
        );

        if (affected < 1) {
            log.warn("支付状态更新失败，可能已被处理, payNo={}", payNo);
            return;
        }

        // 更新订单状态为已支付
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null && OrderStatusEnum.PENDING_PAY.getCode().equals(order.getStatus())) {
            order.setStatus(OrderStatusEnum.PAID.getCode());
            order.setPayType(payRecord.getPayMethod());
            order.setPayTime(payTime);
            order.setUpdateTime(payTime);
            orderMapper.updateById(order);

            log.info("订单支付成功, orderNo={}, payNo={}, amount={}",
                    orderNo, payNo, payRecord.getPayAmount());

            // 发送支付成功消息
            sendPaySuccessMessage(orderNo, payNo);
        }

        // 更新缓存
        updatePayStatusCache(orderNo, PayStatusEnum.PAID.getCode());
    }

    @Override
    public PayStatusResponse queryPayStatus(String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResponseCodeEnum.UNAUTHORIZED);
        }

        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "订单编号不能为空");
        }

        // 先查缓存
        String cacheKey = PAY_STATUS_CACHE_PREFIX + orderNo;
        String cachedStatus = redisOperations.get(cacheKey);
        if (cachedStatus != null) {
            // 缓存命中，直接返回
            PayRecord payRecord = payRecordMapper.selectByOrderNo(orderNo);
            if (payRecord != null) {
                return buildPayStatusResponse(payRecord);
            }
        }

        // 查询订单
        Order order = orderMapper.selectOwnedOrder(orderNo, userId);
        if (order == null) {
            throw new BusinessException(ResponseCodeEnum.ORDER_NOT_FOUND);
        }

        // 查询支付记录
        PayRecord payRecord = payRecordMapper.selectByOrderNo(orderNo);
        if (payRecord == null) {
            // 未创建支付记录
            PayStatusResponse response = new PayStatusResponse();
            response.setOrderNo(orderNo);
            response.setStatus(PayStatusEnum.PENDING.getCode());
            response.setStatusDesc(PayStatusEnum.PENDING.getDesc());
            return response;
        }

        // 更新缓存
        updatePayStatusCache(orderNo, payRecord.getStatus());

        return buildPayStatusResponse(payRecord);
    }

    /**
     * 生成支付流水号
     */
    private String generatePayNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long id = snowflakeIdWorker.nextId();
        return "PAY" + dateStr + String.format("%010d", id % 10000000000L);
    }

    /**
     * 构建支付创建响应
     */
    private PayCreateResponse buildPayCreateResponse(PayRecord payRecord, Order order) {
        PayCreateResponse response = new PayCreateResponse();
        response.setPayNo(payRecord.getPayNo());
        response.setOrderNo(payRecord.getOrderNo());
        response.setPayAmount(payRecord.getPayAmount());
        response.setPayType(payRecord.getPayMethod());

        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payRecord.getPayMethod());
        response.setPayTypeDesc(payTypeEnum != null ? payTypeEnum.getDesc() : "未知");

        // 计算支付过期时间
        if (order.getCreateTime() != null) {
            response.setExpireTime(order.getCreateTime().plusMinutes(ORDER_TIMEOUT_MINUTES));
        }

        // 模拟支付参数
        PayCreateResponse.PayParams payParams = new PayCreateResponse.PayParams();
        payParams.setPayUrl("/api/pay/mock/" + payRecord.getPayNo());
        payParams.setQrCode("https://mock.seckill.com/pay/qr/" + payRecord.getPayNo());
        response.setPayParams(payParams);

        return response;
    }

    /**
     * 构建支付状态响应
     */
    private PayStatusResponse buildPayStatusResponse(PayRecord payRecord) {
        PayStatusResponse response = new PayStatusResponse();
        response.setOrderNo(payRecord.getOrderNo());
        response.setPayNo(payRecord.getPayNo());
        response.setPayAmount(payRecord.getPayAmount());
        response.setPayType(payRecord.getPayMethod());

        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payRecord.getPayMethod());
        response.setPayTypeDesc(payTypeEnum != null ? payTypeEnum.getDesc() : "未知");

        response.setTradeNo(payRecord.getTradeNo());
        response.setStatus(payRecord.getStatus());

        PayStatusEnum statusEnum = PayStatusEnum.getByCode(payRecord.getStatus());
        response.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");

        response.setPayTime(payRecord.getPayTime());

        return response;
    }

    /**
     * 发送支付成功消息
     */
    private void sendPaySuccessMessage(String orderNo, String payNo) {
        try {
            rabbitOperations.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_PAY_SUCCESS_ROUTING_KEY,
                    new PaySuccessMessage(orderNo, payNo)
            );
            log.info("支付成功消息发送成功, orderNo={}, payNo={}", orderNo, payNo);
        } catch (Exception e) {
            log.error("支付成功消息发送失败, orderNo={}, payNo={}", orderNo, payNo, e);
            // 不影响主流程
        }
    }

    /**
     * 更新支付状态缓存
     */
    private void updatePayStatusCache(String orderNo, Integer status) {
        try {
            String cacheKey = PAY_STATUS_CACHE_PREFIX + orderNo;
            redisOperations.set(
                    cacheKey,
                    String.valueOf(status),
                    PAY_STATUS_CACHE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("更新支付状态缓存失败, orderNo={}", orderNo, e);
            // 不影响主流程
        }
    }

    /**
     * 支付成功消息内部类
     */
    private record PaySuccessMessage(String orderNo, String payNo) {
    }
}
