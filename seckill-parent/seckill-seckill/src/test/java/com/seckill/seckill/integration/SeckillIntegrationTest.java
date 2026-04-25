package com.seckill.seckill.integration;

import com.seckill.common.enums.SeckillRecordStatusEnum;
import com.seckill.seckill.dto.SeckillExecuteRequest;
import com.seckill.seckill.dto.SeckillExecuteResponse;
import com.seckill.seckill.dto.SeckillPathResponse;
import com.seckill.seckill.dto.SeckillResultResponse;
import com.seckill.seckill.service.SeckillService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 秒杀模块集成测试
 * <p>
 * 测试完整秒杀链路：获取地址 -> 执行秒杀 -> 查询结果
 * 需要提前准备测试数据（活动、商品、库存等）
 */
@Disabled("Integration test requires running Spring context with Redis/RabbitMQ/MySQL")
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {"/sql/test-seckill-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {"/sql/cleanup-seckill-data.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("秒杀模块集成测试")
class SeckillIntegrationTest {

    @Autowired
    private SeckillService seckillService;

    @Test
    @DisplayName("完整秒杀链路测试 - 单用户正常流程")
    void fullSeckillFlow_SingleUser() throws InterruptedException {
        Long userId = 1001L;
        Long activityId = 1L;
        Long goodsId = 2001L;
        Long addressId = 3001L;

        // Step 1: 获取动态秒杀地址
        SeckillPathResponse pathResponse = seckillService.getSeckillPath(userId, activityId, goodsId);
        assertNotNull(pathResponse);
        assertNotNull(pathResponse.getSeckillPath());
        assertNotNull(pathResponse.getExpiresAt());

        // Step 2: 执行秒杀
        SeckillExecuteRequest request = new SeckillExecuteRequest();
        request.setActivityId(activityId);
        request.setGoodsId(goodsId);
        request.setSeckillPath(pathResponse.getSeckillPath());
        request.setAddressId(addressId);
        request.setQuantity(1);

        SeckillExecuteResponse executeResponse = seckillService.execute(userId, request);
        assertNotNull(executeResponse);
        assertNotNull(executeResponse.getRecordId());
        assertEquals(SeckillRecordStatusEnum.QUEUING.getCode(), executeResponse.getStatus());

        // Step 3: 等待 MQ 处理完成
        Thread.sleep(2000);

        // Step 4: 查询秒杀结果
        SeckillResultResponse resultResponse = seckillService.getResult(userId, executeResponse.getRecordId());
        assertNotNull(resultResponse);
        assertEquals(SeckillRecordStatusEnum.SUCCESS.getCode(), resultResponse.getStatus());
        assertNotNull(resultResponse.getOrderNo());
    }

    @Test
    @DisplayName("重复秒杀测试 - 同一用户不能重复参与")
    void repeatSeckill_ShouldFail() {
        Long userId = 1001L;
        Long activityId = 1L;
        Long goodsId = 2001L;
        Long addressId = 3001L;

        // 第一次秒杀
        SeckillPathResponse pathResponse = seckillService.getSeckillPath(userId, activityId, goodsId);
        SeckillExecuteRequest request = new SeckillExecuteRequest();
        request.setActivityId(activityId);
        request.setGoodsId(goodsId);
        request.setSeckillPath(pathResponse.getSeckillPath());
        request.setAddressId(addressId);
        request.setQuantity(1);

        seckillService.execute(userId, request);

        // 第二次秒杀应该失败
        assertThrows(Exception.class, () -> seckillService.execute(userId, request));
    }

    @Test
    @DisplayName("并发秒杀测试 - 100 并发抢 100 件商品")
    void concurrentSeckill_100Users_100Stock() throws InterruptedException {
        int concurrentUsers = 100;
        int stock = 100;
        Long activityId = 1L;
        Long goodsId = 2001L;
        Long addressId = 3001L;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(concurrentUsers);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentUsers; i++) {
            final Long userId = 2000L + i; // 不同用户
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待统一开始

                    // 获取地址
                    SeckillPathResponse pathResponse = seckillService.getSeckillPath(userId, activityId, goodsId);
                    
                    // 执行秒杀
                    SeckillExecuteRequest request = new SeckillExecuteRequest();
                    request.setActivityId(activityId);
                    request.setGoodsId(goodsId);
                    request.setSeckillPath(pathResponse.getSeckillPath());
                    request.setAddressId(addressId);
                    request.setQuantity(1);

                    try {
                        seckillService.execute(userId, request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 统一开始
        startLatch.countDown();
        
        // 等待所有线程完成
        boolean completed = completeLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "测试超时");
        executor.shutdown();

        // 等待 MQ 处理完成
        Thread.sleep(5000);

        // 验证结果：成功数应该等于库存数
        System.out.println("成功数: " + successCount.get());
        System.out.println("失败数: " + failCount.get());
        
        assertEquals(stock, successCount.get(), "成功数应该等于库存数");
        assertEquals(concurrentUsers - stock, failCount.get(), "失败数应该等于并发数减去库存数");
    }

    @Test
    @DisplayName("无效秒杀地址测试 - 使用错误地址应该失败")
    void invalidSeckillPath_ShouldFail() {
        Long userId = 1001L;
        Long activityId = 1L;
        Long goodsId = 2001L;
        Long addressId = 3001L;

        SeckillExecuteRequest request = new SeckillExecuteRequest();
        request.setActivityId(activityId);
        request.setGoodsId(goodsId);
        request.setSeckillPath("/invalid-path"); // 无效地址
        request.setAddressId(addressId);
        request.setQuantity(1);

        assertThrows(Exception.class, () -> seckillService.execute(userId, request));
    }
}
