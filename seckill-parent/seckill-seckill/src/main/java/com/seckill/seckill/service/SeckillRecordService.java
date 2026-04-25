package com.seckill.seckill.service;

import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.infrastructure.utils.RedisUtils;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import com.seckill.seckill.config.SeckillProperties;
import com.seckill.seckill.dto.SeckillResultResponse;
import com.seckill.seckill.entity.SeckillRecord;
import com.seckill.seckill.mapper.SeckillRecordMapper;
import com.seckill.seckill.support.SeckillRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeckillRecordService {

    private final SeckillRecordMapper seckillRecordMapper;
    private final OrderMapper orderMapper;
    private final RedisUtils redisUtils;
    private final SeckillProperties seckillProperties;

    @Transactional(rollbackFor = Exception.class)
    public SeckillRecord createQueuedRecord(Long userId, Long activityId, Long goodsId) {
        SeckillRecord record = new SeckillRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setGoodsId(goodsId);
        record.setStatus(SeckillRecordStatusEnum.QUEUING.getCode());
        seckillRecordMapper.insert(record);
        return record;
    }

    public void cacheQueuingResult(SeckillRecord record, Goods goods, SeckillGoods seckillGoods) {
        SeckillResultResponse response = buildBaseResponse(record, goods, seckillGoods);
        response.setStatusDesc(SeckillRecordStatusEnum.QUEUING.getDesc());
        cacheResult(response);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long recordId, Long orderId, String orderNo, Goods goods, SeckillGoods seckillGoods) {
        SeckillRecord record = seckillRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        record.setOrderId(orderId);
        record.setStatus(SeckillRecordStatusEnum.SUCCESS.getCode());
        record.setFinishTime(LocalDateTime.now());
        seckillRecordMapper.updateById(record);

        SeckillResultResponse response = buildBaseResponse(record, goods, seckillGoods);
        response.setStatusDesc(SeckillRecordStatusEnum.SUCCESS.getDesc());
        response.setOrderNo(orderNo);
        response.setFinishTime(record.getFinishTime());
        cacheResult(response);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long recordId, String failReason, Goods goods, SeckillGoods seckillGoods) {
        SeckillRecord record = seckillRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        record.setStatus(SeckillRecordStatusEnum.FAILED.getCode());
        record.setFailReason(failReason);
        record.setFinishTime(LocalDateTime.now());
        seckillRecordMapper.updateById(record);

        SeckillResultResponse response = buildBaseResponse(record, goods, seckillGoods);
        response.setStatusDesc(SeckillRecordStatusEnum.FAILED.getDesc());
        response.setFailReason(failReason);
        response.setFinishTime(record.getFinishTime());
        cacheResult(response);
    }

    public SeckillResultResponse getResult(Long userId, Long recordId, Goods goods, SeckillGoods seckillGoods) {
        SeckillResultResponse cached = redisUtils.get(SeckillRedisKeys.result(recordId));
        if (cached != null) {
            return cached;
        }

        SeckillRecord record = seckillRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND, "秒杀记录不存在");
        }

        SeckillResultResponse response = buildBaseResponse(record, goods, seckillGoods);
        response.setFinishTime(record.getFinishTime());
        response.setFailReason(record.getFailReason());
        SeckillRecordStatusEnum statusEnum = SeckillRecordStatusEnum.getByCode(record.getStatus());
        response.setStatusDesc(statusEnum == null ? "未知" : statusEnum.getDesc());
        if (record.getOrderId() != null) {
            Order order = orderMapper.selectById(record.getOrderId());
            if (order != null) {
                response.setOrderNo(order.getOrderNo());
            }
        }
        cacheResult(response);
        return response;
    }

    private SeckillResultResponse buildBaseResponse(SeckillRecord record, Goods goods, SeckillGoods seckillGoods) {
        SeckillResultResponse response = new SeckillResultResponse();
        response.setRecordId(record.getId());
        response.setActivityId(record.getActivityId());
        response.setGoodsId(record.getGoodsId());
        response.setGoodsName(goods == null ? null : goods.getName());
        response.setSeckillPrice(seckillGoods == null ? null : seckillGoods.getSeckillPrice());
        response.setStatus(record.getStatus());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private void cacheResult(SeckillResultResponse response) {
        redisUtils.set(SeckillRedisKeys.result(response.getRecordId()), response, seckillProperties.getResultTtlSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    }
}
