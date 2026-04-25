package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.utils.SnowflakeIdWorker;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.seckill.dto.SeckillExecuteRequest;
import com.seckill.seckill.dto.SeckillExecuteResponse;
import com.seckill.seckill.dto.SeckillPathResponse;
import com.seckill.seckill.dto.SeckillResultResponse;
import com.seckill.seckill.entity.SeckillRecord;
import com.seckill.seckill.mapper.SeckillRecordMapper;
import com.seckill.seckill.mq.SeckillOrderMessage;
import com.seckill.seckill.mq.SeckillOrderProducer;
import com.seckill.user.entity.Address;
import com.seckill.user.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillService {

    private final SeckillPathService seckillPathService;
    private final StockService stockService;
    private final SeckillRecordService seckillRecordService;
    private final AddressService addressService;
    private final GoodsMapper goodsMapper;
    private final SeckillOrderProducer seckillOrderProducer;
    private final SeckillRecordMapper seckillRecordMapper;

    @Autowired
    public SeckillService(SeckillPathService seckillPathService,
                          StockService stockService,
                          SeckillRecordService seckillRecordService,
                          AddressService addressService,
                          GoodsMapper goodsMapper,
                          SeckillOrderProducer seckillOrderProducer,
                          SeckillRecordMapper seckillRecordMapper) {
        this.seckillPathService = seckillPathService;
        this.stockService = stockService;
        this.seckillRecordService = seckillRecordService;
        this.addressService = addressService;
        this.goodsMapper = goodsMapper;
        this.seckillOrderProducer = seckillOrderProducer;
        this.seckillRecordMapper = seckillRecordMapper;
    }

    public SeckillPathResponse getSeckillPath(Long userId, Long activityId, Long goodsId) {
        return seckillPathService.createPath(userId, activityId, goodsId);
    }

    public SeckillExecuteResponse execute(Long userId, SeckillExecuteRequest request) {
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity != 1) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "M4 阶段仅支持每次购买 1 件");
        }

        SeckillActivity activity = seckillPathService.requireActivity(request.getActivityId());
        seckillPathService.validateExecuteWindow(activity);
        SeckillGoods seckillGoods = seckillPathService.requireSeckillGoods(request.getActivityId(), request.getGoodsId());
        if (seckillGoods.getLimitPerUser() != null && quantity > seckillGoods.getLimitPerUser()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST, "超过活动限购数量");
        }

        seckillPathService.validatePath(userId, request.getActivityId(), request.getGoodsId(), request.getSeckillPath());
        Address address = addressService.getAddressById(userId, request.getAddressId());
        Goods goods = goodsMapper.selectById(request.getGoodsId());
        if (goods == null) {
            throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
        }

        stockService.preDeduct(request.getActivityId(), request.getGoodsId(), userId, quantity);

        SeckillRecord record = null;
        try {
            record = seckillRecordService.createQueuedRecord(userId, request.getActivityId(), request.getGoodsId());
            seckillRecordService.cacheQueuingResult(record, goods, seckillGoods);

            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setRecordId(record.getId());
            message.setOrderNo(SnowflakeIdWorker.getInstance().generateOrderNo());
            message.setUserId(userId);
            message.setActivityId(request.getActivityId());
            message.setGoodsId(request.getGoodsId());
            message.setQuantity(quantity);
            message.setAddressId(request.getAddressId());
            message.setReceiverName(address.getReceiverName());
            message.setReceiverPhone(address.getReceiverPhone());
            message.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
            seckillOrderProducer.send(message);
        } catch (RuntimeException ex) {
            stockService.rollback(request.getActivityId(), request.getGoodsId(), userId, quantity);
            if (record != null) {
                seckillRecordService.markFailed(record.getId(), "秒杀请求提交失败", goods, seckillGoods);
            }
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ResponseCodeEnum.MQ_ERROR, "秒杀请求提交失败，请稍后重试");
        }

        SeckillExecuteResponse response = new SeckillExecuteResponse();
        response.setRecordId(record.getId());
        response.setStatus(SeckillRecordStatusEnum.QUEUING.getCode());
        response.setStatusDesc(SeckillRecordStatusEnum.QUEUING.getDesc());
        response.setMessage("您的秒杀请求已进入处理队列，请稍后查询结果");
        return response;
    }

    public SeckillResultResponse getResult(Long userId, Long recordId) {
        SeckillRecord record = seckillRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND, "秒杀记录不存在");
        }
        Goods goods = goodsMapper.selectById(record.getGoodsId());
        SeckillGoods seckillGoods = seckillPathService.requireSeckillGoods(record.getActivityId(), record.getGoodsId());
        return seckillRecordService.getResult(userId, recordId, goods, seckillGoods);
    }
}
