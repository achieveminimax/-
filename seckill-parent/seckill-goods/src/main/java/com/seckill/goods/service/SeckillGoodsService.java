package com.seckill.goods.service;

import com.seckill.common.result.PageResult;
import com.seckill.goods.dto.SeckillActivityDetailResponse;
import com.seckill.goods.dto.SeckillActivityListResponse;

/**
 * 秒杀活动只读服务接口。
 */
public interface SeckillGoodsService {

    /**
     * 查询秒杀活动列表。
     */
    PageResult<SeckillActivityListResponse> getActivityList(Integer status, Long current, Long size);

    /**
     * 查询单个秒杀活动详情。
     */
    SeckillActivityDetailResponse getActivityDetail(Long activityId);
}
