package com.seckill.admin.service;

import com.seckill.admin.dto.seckill.AdminSeckillActivityDetailResponse;
import com.seckill.admin.dto.seckill.AdminSeckillActivityRequest;
import com.seckill.admin.dto.seckill.AdminSeckillActivityResponse;
import com.seckill.admin.dto.seckill.AdminSeckillStatisticsResponse;
import com.seckill.common.result.PageResult;

/**
 * 管理端秒杀活动服务接口。
 */
public interface AdminSeckillService {

    /**
     * 获取秒杀活动列表。
     *
     * @param status  活动状态筛选
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页活动列表
     */
    PageResult<AdminSeckillActivityResponse> getActivityList(Integer status, Long current, Long size);

    /**
     * 获取秒杀活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    AdminSeckillActivityDetailResponse getActivityDetail(Long activityId);

    /**
     * 创建秒杀活动。
     *
     * @param request 活动创建请求
     * @return 创建的活动 ID
     */
    Long createActivity(AdminSeckillActivityRequest request);

    /**
     * 修改秒杀活动。
     *
     * @param request 活动修改请求
     */
    void updateActivity(AdminSeckillActivityRequest request);

    /**
     * 获取秒杀活动统计。
     *
     * @param activityId 活动 ID
     * @return 活动统计数据
     */
    AdminSeckillStatisticsResponse getActivityStatistics(Long activityId);
}
