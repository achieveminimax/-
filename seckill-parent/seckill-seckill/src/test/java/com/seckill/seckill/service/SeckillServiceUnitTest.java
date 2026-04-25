package com.seckill.seckill.service;

import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.common.exception.BusinessException;
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
import com.seckill.seckill.mq.SeckillOrderProducer;
import com.seckill.user.entity.Address;
import com.seckill.user.service.AddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillService 单元测试")
class SeckillServiceUnitTest {

    @Mock
    private SeckillPathService seckillPathService;

    @Mock
    private StockService stockService;

    @Mock
    private SeckillRecordService seckillRecordService;

    @Mock
    private AddressService addressService;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private SeckillOrderProducer seckillOrderProducer;

    @Mock
    private SeckillRecordMapper seckillRecordMapper;

    @Test
    @DisplayName("执行秒杀失败 - M4 仅支持单件购买")
    void execute_Fail_WhenQuantityNotOne() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        request.setQuantity(2);

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
        verify(stockService, never()).preDeduct(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("执行秒杀失败 - 地址不属于当前用户")
    void execute_Fail_WhenAddressNotOwned() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenThrow(new BusinessException(ResponseCodeEnum.ADDRESS_NOT_FOUND));

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.ADDRESS_NOT_FOUND.getCode(), exception.getCode());
        verify(stockService, never()).preDeduct(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("执行秒杀失败 - MQ 发送异常时回滚库存并回写失败结果")
    void execute_Fail_WhenMqSendError() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();
        Goods goods = goods();
        Address address = address();
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenReturn(address);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        doNothing().when(stockService).preDeduct(1L, 2001L, 1001L, 1);
        when(seckillRecordService.createQueuedRecord(1001L, 1L, 2001L)).thenReturn(record);
        doNothing().when(seckillRecordService).cacheQueuingResult(record, goods, seckillGoods);
        doThrow(new RuntimeException("mq down")).when(seckillOrderProducer).send(any());

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.MQ_ERROR.getCode(), exception.getCode());
        verify(stockService).rollback(1L, 2001L, 1001L, 1);
        verify(seckillRecordService).markFailed(9001L, "秒杀请求提交失败", goods, seckillGoods);
    }

    @Test
    @DisplayName("执行秒杀成功 - 返回排队结果")
    void execute_Success() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();
        Goods goods = goods();
        Address addr = address();
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenReturn(addr);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        doNothing().when(stockService).preDeduct(1L, 2001L, 1001L, 1);
        when(seckillRecordService.createQueuedRecord(1001L, 1L, 2001L)).thenReturn(record);
        doNothing().when(seckillRecordService).cacheQueuingResult(record, goods, seckillGoods);
        doNothing().when(seckillOrderProducer).send(any());

        SeckillExecuteResponse response = seckillService.execute(1001L, request);

        assertNotNull(response);
        assertEquals(9001L, response.getRecordId());
        assertEquals(SeckillRecordStatusEnum.QUEUING.getCode(), response.getStatus());
    }

    @Test
    @DisplayName("执行秒杀失败 - 商品不存在")
    void execute_Fail_GoodsNotFound() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenReturn(address());
        when(goodsMapper.selectById(2001L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("执行秒杀失败 - 超过活动限购数量")
    void execute_Fail_ExceedLimitPerUser() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        request.setQuantity(1);
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();
        seckillGoods.setLimitPerUser(0);

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("执行秒杀失败 - quantity为null时默认使用1")
    void execute_Success_QuantityNullDefaultsToOne() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        request.setQuantity(null);
        SeckillActivity activity = activity();
        SeckillGoods seckillGoods = seckillGoods();
        Goods goods = goods();
        Address addr = address();
        SeckillRecord record = new SeckillRecord();
        record.setId(9001L);

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(seckillGoods);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenReturn(addr);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        doNothing().when(stockService).preDeduct(1L, 2001L, 1001L, 1);
        when(seckillRecordService.createQueuedRecord(1001L, 1L, 2001L)).thenReturn(record);
        doNothing().when(seckillRecordService).cacheQueuingResult(record, goods, seckillGoods);
        doNothing().when(seckillOrderProducer).send(any());

        SeckillExecuteResponse response = seckillService.execute(1001L, request);

        assertNotNull(response);
        verify(stockService).preDeduct(1L, 2001L, 1001L, 1);
    }

    @Test
    @DisplayName("getSeckillPath - 委托给SeckillPathService")
    void getSeckillPath_DelegatesToPathService() {
        SeckillService seckillService = buildService();
        SeckillPathResponse expected = new SeckillPathResponse();
        expected.setSeckillPath("/abc123");
        when(seckillPathService.createPath(1001L, 1L, 2001L)).thenReturn(expected);

        SeckillPathResponse result = seckillService.getSeckillPath(1001L, 1L, 2001L);

        assertEquals("/abc123", result.getSeckillPath());
    }

    @Test
    @DisplayName("getResult - 秒杀记录不存在抛异常")
    void getResult_Fail_RecordNotFound() {
        SeckillService seckillService = buildService();
        when(seckillRecordMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillService.getResult(1001L, 999L));

        assertEquals(ResponseCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("getResult - 秒杀记录不属于当前用户抛异常")
    void getResult_Fail_RecordNotOwned() {
        SeckillService seckillService = buildService();
        SeckillRecord record = new SeckillRecord();
        record.setId(1L);
        record.setUserId(9999L);
        when(seckillRecordMapper.selectById(1L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillService.getResult(1001L, 1L));

        assertEquals(ResponseCodeEnum.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("getResult - 成功查询秒杀结果")
    void getResult_Success() {
        SeckillService seckillService = buildService();
        SeckillRecord record = new SeckillRecord();
        record.setId(1L);
        record.setUserId(1001L);
        record.setActivityId(1L);
        record.setGoodsId(2001L);

        SeckillGoods sg = seckillGoods();
        Goods goods = goods();
        SeckillResultResponse expected = new SeckillResultResponse();
        expected.setRecordId(1L);
        expected.setStatus(SeckillRecordStatusEnum.SUCCESS.getCode());

        when(seckillRecordMapper.selectById(1L)).thenReturn(record);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(sg);
        when(seckillRecordService.getResult(1001L, 1L, goods, sg)).thenReturn(expected);

        SeckillResultResponse result = seckillService.getResult(1001L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getRecordId());
        assertEquals(SeckillRecordStatusEnum.SUCCESS.getCode(), result.getStatus());
    }

    @Test
    @DisplayName("执行秒杀失败 - BusinessException在try块内抛出时仍回滚库存")
    void execute_Fail_BusinessExceptionInTryBlock_RollsBackStock() {
        SeckillService seckillService = buildService();
        SeckillExecuteRequest request = request();
        SeckillActivity activity = activity();
        SeckillGoods sg = seckillGoods();
        Goods goods = goods();
        Address addr = address();

        when(seckillPathService.requireActivity(1L)).thenReturn(activity);
        doNothing().when(seckillPathService).validateExecuteWindow(activity);
        when(seckillPathService.requireSeckillGoods(1L, 2001L)).thenReturn(sg);
        doNothing().when(seckillPathService).validatePath(1001L, 1L, 2001L, "/valid-path");
        when(addressService.getAddressById(1001L, 3001L)).thenReturn(addr);
        when(goodsMapper.selectById(2001L)).thenReturn(goods);
        doNothing().when(stockService).preDeduct(1L, 2001L, 1001L, 1);
        when(seckillRecordService.createQueuedRecord(1001L, 1L, 2001L))
                .thenThrow(new BusinessException(ResponseCodeEnum.ERROR));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> seckillService.execute(1001L, request));

        assertEquals(ResponseCodeEnum.ERROR.getCode(), exception.getCode());
        verify(stockService).rollback(1L, 2001L, 1001L, 1);
    }

    private SeckillService buildService() {
        return new SeckillService(
                seckillPathService,
                stockService,
                seckillRecordService,
                addressService,
                goodsMapper,
                seckillOrderProducer,
                seckillRecordMapper
        );
    }

    private SeckillExecuteRequest request() {
        SeckillExecuteRequest request = new SeckillExecuteRequest();
        request.setActivityId(1L);
        request.setGoodsId(2001L);
        request.setSeckillPath("/valid-path");
        request.setAddressId(3001L);
        request.setQuantity(1);
        return request;
    }

    private SeckillActivity activity() {
        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setStartTime(LocalDateTime.now().minusMinutes(5));
        activity.setEndTime(LocalDateTime.now().plusMinutes(10));
        return activity;
    }

    private SeckillGoods seckillGoods() {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setActivityId(1L);
        seckillGoods.setGoodsId(2001L);
        seckillGoods.setLimitPerUser(1);
        seckillGoods.setSeckillPrice(new BigDecimal("99.00"));
        return seckillGoods;
    }

    private Goods goods() {
        Goods goods = new Goods();
        goods.setId(2001L);
        goods.setName("测试商品");
        return goods;
    }

    private Address address() {
        Address address = new Address();
        address.setId(3001L);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetailAddress("科技园 1 号");
        return address;
    }
}
