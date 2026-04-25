package com.seckill.goods.task;

import com.seckill.goods.service.SeckillPreheatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillPreheatTask 单元测试")
class SeckillPreheatTaskUnitTest {

    @Mock
    private SeckillPreheatService seckillPreheatService;

    @InjectMocks
    private SeckillPreheatTask seckillPreheatTask;

    @Test
    @DisplayName("定时任务调用预热方法")
    void preheatScheduled_Success() {
        when(seckillPreheatService.preheatUpcomingActivities()).thenReturn(2);

        seckillPreheatTask.preheatUpcomingActivities();

        verify(seckillPreheatService).preheatUpcomingActivities();
    }

    @Test
    @DisplayName("定时任务 - 无活动需要预热")
    void preheatScheduled_NoActivities() {
        when(seckillPreheatService.preheatUpcomingActivities()).thenReturn(0);

        seckillPreheatTask.preheatUpcomingActivities();

        verify(seckillPreheatService).preheatUpcomingActivities();
    }
}
