package com.seckill.goods.service;

/**
 * 秒杀活动预热服务接口。
 * <p>
 * 负责在活动开始前把活动详情和库存加载到 Redis，降低活动开始瞬间的数据库压力。
 */
public interface SeckillPreheatService {

    /**
     * 批量预热未来 5 分钟内即将开始的活动。
     *
     * @return 本次成功预热的活动数量
     */
    int preheatUpcomingActivities();

    /**
     * 手动预热单个活动。
     *
     * @param activityId 活动 ID
     */
    void preheatActivity(Long activityId);
}
