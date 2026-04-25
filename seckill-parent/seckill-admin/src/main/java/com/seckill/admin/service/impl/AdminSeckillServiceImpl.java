package com.seckill.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.admin.dto.seckill.AdminSeckillActivityDetailResponse;
import com.seckill.admin.dto.seckill.AdminSeckillActivityRequest;
import com.seckill.admin.dto.seckill.AdminSeckillActivityResponse;
import com.seckill.admin.dto.seckill.AdminSeckillGoodsItemRequest;
import com.seckill.admin.dto.seckill.AdminSeckillStatisticsResponse;
import com.seckill.admin.service.AdminSeckillService;
import com.seckill.common.enums.ResponseCodeEnum;
import com.seckill.common.exception.BusinessException;
import com.seckill.common.result.PageResult;
import com.seckill.goods.entity.Goods;
import com.seckill.goods.entity.SeckillActivity;
import com.seckill.goods.entity.SeckillGoods;
import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillActivityMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端秒杀活动服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminSeckillServiceImpl implements AdminSeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final GoodsMapper goodsMapper;
    private final RedisUtils redisUtils;

    @Override
    public PageResult<AdminSeckillActivityResponse> getActivityList(Integer status, Long current, Long size) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        
        // 根据状态筛选
        if (status != null && status > 0) {
            LocalDateTime now = LocalDateTime.now();
            switch (status) {
                case 1 -> wrapper.gt(SeckillActivity::getStartTime, now); // 未开始
                case 2 -> wrapper.le(SeckillActivity::getStartTime, now)
                               .ge(SeckillActivity::getEndTime, now); // 进行中
                case 3 -> wrapper.lt(SeckillActivity::getEndTime, now); // 已结束
            }
        }
        
        wrapper.orderByDesc(SeckillActivity::getCreateTime);
        
        long total = seckillActivityMapper.selectCount(wrapper);
        long offset = (current - 1) * size;
        
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
                wrapper.last("LIMIT " + offset + ", " + size));
        
        List<AdminSeckillActivityResponse> records = activities.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
        
        PageResult<AdminSeckillActivityResponse> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(size);
        result.setCurrent(current);
        result.setPages((total + size - 1) / size);
        
        return result;
    }

    @Override
    public AdminSeckillActivityDetailResponse getActivityDetail(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }

        return convertToDetailResponse(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(AdminSeckillActivityRequest request) {
        // 校验时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "活动结束时间必须大于开始时间");
        }
        
        // 创建活动
        SeckillActivity activity = new SeckillActivity();
        activity.setActivityName(request.getActivityName());
        activity.setDescription(request.getDescription());
        activity.setActivityImg(request.getActivityImg());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(0);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        
        seckillActivityMapper.insert(activity);
        
        // 创建活动商品
        createSeckillGoods(activity.getId(), request.getGoodsList());
        
        return activity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(AdminSeckillActivityRequest request) {
        if (request.getActivityId() == null) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "活动ID不能为空");
        }
        
        SeckillActivity activity = seckillActivityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }

        // 进行中的活动不允许修改
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime().isBefore(now) && activity.getEndTime().isAfter(now)) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "进行中的活动不允许修改");
        }
        
        // 校验时间
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, "活动结束时间必须大于开始时间");
        }
        
        // 更新活动
        activity.setActivityName(request.getActivityName());
        activity.setDescription(request.getDescription());
        activity.setActivityImg(request.getActivityImg());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setUpdateTime(LocalDateTime.now());
        
        seckillActivityMapper.updateById(activity);
        
        // 删除旧商品关联，创建新的
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillGoods::getActivityId, activity.getId());
        seckillGoodsMapper.delete(wrapper);
        
        createSeckillGoods(activity.getId(), request.getGoodsList());
    }

    @Override
    public AdminSeckillStatisticsResponse getActivityStatistics(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResponseCodeEnum.SECKILL_NOT_FOUND);
        }
        
        // 获取活动商品
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillGoods::getActivityId, activityId);
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        
        // 计算统计信息
        int totalStock = seckillGoodsList.stream().mapToInt(SeckillGoods::getSeckillStock).sum();
        int totalSales = seckillGoodsList.stream().mapToInt(SeckillGoods::getSalesCount).sum();
        
        AdminSeckillStatisticsResponse response = new AdminSeckillStatisticsResponse();
        response.setActivityId(activityId);
        response.setActivityName(activity.getActivityName());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setTotalStock(totalStock);
        response.setTotalSales(totalSales);
        response.setSalesRate(totalStock > 0 ? 
                BigDecimal.valueOf(totalSales).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalStock), 2, RoundingMode.HALF_UP).doubleValue() : 0.0);
        
        // 计算销售额
        BigDecimal totalAmount = seckillGoodsList.stream()
                .map(g -> g.getSeckillPrice().multiply(BigDecimal.valueOf(g.getSalesCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(totalAmount);
        
        // 商品统计
        List<AdminSeckillStatisticsResponse.GoodsStatistics> goodsStats = seckillGoodsList.stream()
                .map(g -> {
                    AdminSeckillStatisticsResponse.GoodsStatistics stat = 
                            new AdminSeckillStatisticsResponse.GoodsStatistics();
                    stat.setGoodsId(g.getGoodsId());
                    
                    Goods goods = goodsMapper.selectById(g.getGoodsId());
                    stat.setGoodsName(goods != null ? goods.getName() : "未知商品");
                    stat.setSeckillPrice(g.getSeckillPrice());
                    stat.setTotalStock(g.getSeckillStock());
                    stat.setTotalSales(g.getSalesCount());
                    stat.setSalesRate(g.getSeckillStock() > 0 ?
                            BigDecimal.valueOf(g.getSalesCount()).multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(g.getSeckillStock()), 2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                    stat.setTotalAmount(g.getSeckillPrice().multiply(BigDecimal.valueOf(g.getSalesCount())));
                    return stat;
                })
                .collect(Collectors.toList());
        response.setGoodsStatistics(goodsStats);
        
        // 模拟时间分布数据
        response.setTimeDistribution(generateTimeDistribution(activity));
        
        // 模拟请求数据
        response.setTotalRequests((long) totalSales * 20); // 假设转化率 5%
        response.setSuccessRate(5.0);
        response.setAverageResponseTime(156L);
        
        return response;
    }

    private void createSeckillGoods(Long activityId, List<AdminSeckillGoodsItemRequest> goodsList) {
        for (AdminSeckillGoodsItemRequest item : goodsList) {
            // 校验商品
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods == null) {
                throw new BusinessException(ResponseCodeEnum.GOODS_NOT_FOUND);
            }
            
            // 校验秒杀价格
            if (item.getSeckillPrice().compareTo(goods.getPrice()) >= 0) {
                throw new BusinessException(ResponseCodeEnum.PARAM_ERROR, 
                        "商品[" + goods.getName() + "]的秒杀价格必须小于原价");
            }
            
            // 校验库存
            if (item.getSeckillStock() > goods.getStock()) {
                throw new BusinessException(ResponseCodeEnum.PARAM_ERROR,
                        "商品[" + goods.getName() + "]的秒杀库存不能超过商品总库存");
            }
            
            SeckillGoods seckillGoods = new SeckillGoods();
            seckillGoods.setActivityId(activityId);
            seckillGoods.setGoodsId(item.getGoodsId());
            seckillGoods.setSeckillPrice(item.getSeckillPrice());
            seckillGoods.setSeckillStock(item.getSeckillStock());
            seckillGoods.setLimitPerUser(item.getLimitPerUser());
            seckillGoods.setSalesCount(0);
            seckillGoods.setCreateTime(LocalDateTime.now());
            seckillGoods.setUpdateTime(LocalDateTime.now());
            
            seckillGoodsMapper.insert(seckillGoods);
        }
    }

    private AdminSeckillActivityResponse convertToListResponse(SeckillActivity activity) {
        AdminSeckillActivityResponse response = new AdminSeckillActivityResponse();
        response.setActivityId(activity.getId());
        response.setActivityName(activity.getActivityName());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        
        // 计算状态
        LocalDateTime now = LocalDateTime.now();
        int status;
        String statusDesc;
        if (now.isBefore(activity.getStartTime())) {
            status = 1;
            statusDesc = "未开始";
        } else if (now.isAfter(activity.getEndTime())) {
            status = 3;
            statusDesc = "已结束";
        } else {
            status = 2;
            statusDesc = "进行中";
        }
        response.setStatus(status);
        response.setStatusDesc(statusDesc);
        
        // 查询商品统计
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillGoods::getActivityId, activity.getId());
        List<SeckillGoods> goodsList = seckillGoodsMapper.selectList(wrapper);
        
        response.setGoodsCount(goodsList.size());
        response.setTotalStock(goodsList.stream().mapToInt(SeckillGoods::getSeckillStock).sum());
        response.setTotalSales(goodsList.stream().mapToInt(SeckillGoods::getSalesCount).sum());
        response.setCreateTime(activity.getCreateTime());
        
        return response;
    }

    private AdminSeckillActivityDetailResponse convertToDetailResponse(SeckillActivity activity) {
        AdminSeckillActivityDetailResponse response = new AdminSeckillActivityDetailResponse();
        response.setActivityId(activity.getId());
        response.setActivityName(activity.getActivityName());
        response.setDescription(activity.getDescription());
        response.setActivityImg(activity.getActivityImg());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        
        // 计算状态
        LocalDateTime now = LocalDateTime.now();
        int status;
        String statusDesc;
        if (now.isBefore(activity.getStartTime())) {
            status = 1;
            statusDesc = "未开始";
        } else if (now.isAfter(activity.getEndTime())) {
            status = 3;
            statusDesc = "已结束";
        } else {
            status = 2;
            statusDesc = "进行中";
        }
        response.setStatus(status);
        response.setStatusDesc(statusDesc);
        
        // 默认规则
        response.setRules(List.of(
                "每人限购 1 件",
                "秒杀商品不支持使用优惠券",
                "秒杀订单需在 15 分钟内完成支付",
                "秒杀成功后不可取消订单"
        ));
        
        // 查询商品
        LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillGoods::getActivityId, activity.getId());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        
        List<AdminSeckillActivityDetailResponse.AdminSeckillGoodsDetailResponse> goodsResponses = 
                seckillGoodsList.stream().map(g -> {
            AdminSeckillActivityDetailResponse.AdminSeckillGoodsDetailResponse gr = 
                    new AdminSeckillActivityDetailResponse.AdminSeckillGoodsDetailResponse();
            gr.setGoodsId(g.getGoodsId());
            
            Goods goods = goodsMapper.selectById(g.getGoodsId());
            if (goods != null) {
                gr.setGoodsName(goods.getName());
                gr.setGoodsImg(goods.getCoverImage());
                gr.setOriginalPrice(goods.getPrice());
            }
            gr.setSeckillPrice(g.getSeckillPrice());
            gr.setSeckillStock(g.getSeckillStock());
            gr.setSalesCount(g.getSalesCount());
            gr.setLimitPerUser(g.getLimitPerUser());
            return gr;
        }).collect(Collectors.toList());
        
        response.setGoodsList(goodsResponses);
        response.setCreateTime(activity.getCreateTime());
        response.setUpdateTime(activity.getUpdateTime());
        
        return response;
    }

    private List<AdminSeckillStatisticsResponse.TimeDistribution> generateTimeDistribution(SeckillActivity activity) {
        List<AdminSeckillStatisticsResponse.TimeDistribution> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        LocalDateTime start = activity.getStartTime();
        LocalDateTime end = activity.getEndTime();
        
        // 生成每分钟的分布数据（模拟数据）
        int totalMinutes = (int) java.time.Duration.between(start, end).toMinutes();
        for (int i = 0; i < Math.min(totalMinutes, 10); i++) {
            AdminSeckillStatisticsResponse.TimeDistribution dist = 
                    new AdminSeckillStatisticsResponse.TimeDistribution();
            dist.setTime(start.plusMinutes(i).format(formatter));
            // 模拟递减的销售数据
            dist.setSales(Math.max(1, 50 - i * 5));
            list.add(dist);
        }
        
        return list;
    }
}
