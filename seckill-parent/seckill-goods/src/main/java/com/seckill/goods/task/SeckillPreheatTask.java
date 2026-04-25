package com.seckill.goods.task;

import com.seckill.goods.service.SeckillPreheatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 秒杀预热定时任务。
 * <p>
 * 每分钟扫描一次即将开始的活动，并把活动详情和库存提前装载到 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillPreheatTask {

    /**
     * 预热服务。
     */
    private final SeckillPreheatService seckillPreheatService;

    /**
     * 定时预热未来 5 分钟内的活动。
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void preheatUpcomingActivities() {
        int count = seckillPreheatService.preheatUpcomingActivities();
        if (count > 0) {
            log.info("定时预热完成，预热活动数量={}", count);
        }
    }
}
