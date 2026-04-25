package com.seckill.admin.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.seckill.admin.dto.seckill.AdminSeckillActivityDetailResponse;
import com.seckill.admin.dto.seckill.AdminSeckillActivityRequest;
import com.seckill.admin.dto.seckill.AdminSeckillActivityResponse;
import com.seckill.admin.dto.seckill.AdminSeckillGoodsItemRequest;
import com.seckill.admin.dto.seckill.AdminSeckillStatisticsResponse;
import com.seckill.admin.service.impl.AdminSeckillServiceImpl;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSeckillService 单元测试")
class AdminSeckillServiceImplUnitTest {

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SeckillActivity.class);
        TableInfoHelper.initTableInfo(assistant, SeckillGoods.class);
        TableInfoHelper.initTableInfo(assistant, Goods.class);
    }

    @Mock
    private SeckillActivityMapper seckillActivityMapper;

    @Mock
    private SeckillGoodsMapper seckillGoodsMapper;

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private RedisUtils redisUtils;

    private AdminSeckillServiceImpl adminSeckillService;

    @BeforeEach
    void setUp() {
        adminSeckillService = new AdminSeckillServiceImpl(seckillActivityMapper, seckillGoodsMapper, goodsMapper, redisUtils);
    }

    @Nested
    @DisplayName("getActivityList 测试")
    class GetActivityListTests {

        @Test
        @DisplayName("查询全部活动")
        void getActivityList_AllActivities() {
            SeckillActivity act = activity(1L, "限时秒杀",
                    LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusHours(1));
            when(seckillActivityMapper.selectCount(any())).thenReturn(1L);
            when(seckillActivityMapper.selectList(any())).thenReturn(List.of(act));
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());

            PageResult<AdminSeckillActivityResponse> result = adminSeckillService.getActivityList(null, 1L, 10L);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getRecords().size());
            assertEquals("限时秒杀", result.getRecords().getFirst().getActivityName());
        }

        @Test
        @DisplayName("按状态筛选 - 进行中")
        void getActivityList_FilterByStatus() {
            SeckillActivity act = activity(1L, "进行中秒杀",
                    LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(30));
            when(seckillActivityMapper.selectCount(any())).thenReturn(1L);
            when(seckillActivityMapper.selectList(any())).thenReturn(List.of(act));
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());

            PageResult<AdminSeckillActivityResponse> result = adminSeckillService.getActivityList(2, 1L, 10L);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("分页查询 - 空结果")
        void getActivityList_Pagination_Empty() {
            when(seckillActivityMapper.selectCount(any())).thenReturn(0L);
            when(seckillActivityMapper.selectList(any())).thenReturn(Collections.emptyList());

            PageResult<AdminSeckillActivityResponse> result = adminSeckillService.getActivityList(null, 2L, 10L);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertEquals(0, result.getRecords().size());
        }
    }

    @Nested
    @DisplayName("getActivityDetail 测试")
    class GetActivityDetailTests {

        @Test
        @DisplayName("活动详情查询成功")
        void getActivityDetail_Success() {
            SeckillActivity act = activity(1L, "限时秒杀",
                    LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusHours(1));
            when(seckillActivityMapper.selectById(1L)).thenReturn(act);
            when(seckillGoodsMapper.selectList(any())).thenReturn(Collections.emptyList());

            AdminSeckillActivityDetailResponse result = adminSeckillService.getActivityDetail(1L);

            assertNotNull(result);
            assertEquals(1L, result.getActivityId());
            assertEquals("限时秒杀", result.getActivityName());
            assertEquals(1, result.getStatus());
            assertEquals("未开始", result.getStatusDesc());
        }

        @Test
        @DisplayName("活动不存在")
        void getActivityDetail_NotFound() {
            when(seckillActivityMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.getActivityDetail(999L));
            assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("createActivity 测试")
    class CreateActivityTests {

        @Test
        @DisplayName("创建活动成功")
        void createActivity_Success() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            when(seckillActivityMapper.insert(any(SeckillActivity.class))).thenAnswer(invocation -> {
                SeckillActivity a = invocation.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(goodsMapper.selectById(101L)).thenReturn(goods(101L, "iPhone", new BigDecimal("9999"), 50));
            when(seckillGoodsMapper.insert(any(SeckillGoods.class))).thenReturn(1);

            Long id = adminSeckillService.createActivity(request);

            assertEquals(1L, id);
            verify(seckillActivityMapper).insert(any(SeckillActivity.class));
            verify(seckillGoodsMapper).insert(any(SeckillGoods.class));
        }

        @Test
        @DisplayName("结束时间早于开始时间")
        void createActivity_Fail_EndBeforeStart() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(1));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.createActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("结束时间"));
        }

        @Test
        @DisplayName("商品不存在")
        void createActivity_Fail_GoodsNotFound() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            when(seckillActivityMapper.insert(any(SeckillActivity.class))).thenAnswer(invocation -> {
                SeckillActivity a = invocation.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(goodsMapper.selectById(101L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.createActivity(request));
            assertEquals(ResponseCodeEnum.GOODS_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("秒杀价>=原价")
        void createActivity_Fail_SeckillPriceTooHigh() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.getGoodsList().getFirst().setSeckillPrice(new BigDecimal("9999"));

            when(seckillActivityMapper.insert(any(SeckillActivity.class))).thenAnswer(invocation -> {
                SeckillActivity a = invocation.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(goodsMapper.selectById(101L)).thenReturn(goods(101L, "iPhone", new BigDecimal("9999"), 50));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.createActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("秒杀价格"));
        }

        @Test
        @DisplayName("秒杀库存>商品库存")
        void createActivity_Fail_SeckillStockExceed() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.getGoodsList().getFirst().setSeckillStock(100);

            when(seckillActivityMapper.insert(any(SeckillActivity.class))).thenAnswer(invocation -> {
                SeckillActivity a = invocation.getArgument(0);
                a.setId(1L);
                return 1;
            });
            when(goodsMapper.selectById(101L)).thenReturn(goods(101L, "iPhone", new BigDecimal("9999"), 50));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.createActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("秒杀库存"));
        }
    }

    @Nested
    @DisplayName("updateActivity 测试")
    class UpdateActivityTests {

        @Test
        @DisplayName("更新活动成功")
        void updateActivity_Success() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.setActivityId(1L);

            SeckillActivity existing = activity(1L, "旧活动",
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            when(seckillActivityMapper.selectById(1L)).thenReturn(existing);
            when(seckillActivityMapper.updateById(any(SeckillActivity.class))).thenReturn(1);
            when(seckillGoodsMapper.delete(any())).thenReturn(0);
            when(goodsMapper.selectById(101L)).thenReturn(goods(101L, "iPhone", new BigDecimal("9999"), 50));
            when(seckillGoodsMapper.insert(any(SeckillGoods.class))).thenReturn(1);

            adminSeckillService.updateActivity(request);

            verify(seckillActivityMapper).updateById(any(SeckillActivity.class));
            verify(seckillGoodsMapper).delete(any());
            verify(seckillGoodsMapper).insert(any(SeckillGoods.class));
        }

        @Test
        @DisplayName("活动ID为空")
        void updateActivity_Fail_NoActivityId() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.setActivityId(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.updateActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("活动ID"));
        }

        @Test
        @DisplayName("活动不存在")
        void updateActivity_Fail_ActivityNotFound() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.setActivityId(999L);

            when(seckillActivityMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.updateActivity(request));
            assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("进行中不允许修改")
        void updateActivity_Fail_OngoingActivity() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
            request.setActivityId(1L);

            SeckillActivity ongoing = activity(1L, "进行中",
                    LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusMinutes(30));
            when(seckillActivityMapper.selectById(1L)).thenReturn(ongoing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.updateActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("进行中"));
        }

        @Test
        @DisplayName("时间校验 - 结束时间早于开始时间")
        void updateActivity_Fail_EndBeforeStart() {
            AdminSeckillActivityRequest request = createActivityRequest(
                    LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(1));
            request.setActivityId(1L);

            SeckillActivity existing = activity(1L, "旧活动",
                    LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(4));
            when(seckillActivityMapper.selectById(1L)).thenReturn(existing);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.updateActivity(request));
            assertEquals(ResponseCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("结束时间"));
        }
    }

    @Nested
    @DisplayName("getActivityStatistics 测试")
    class GetActivityStatisticsTests {

        @Test
        @DisplayName("统计信息查询成功")
        void getActivityStatistics_Success() {
            SeckillActivity act = activity(1L, "限时秒杀",
                    LocalDateTime.now().minusMinutes(30), LocalDateTime.now().plusMinutes(30));
            SeckillGoods sg = new SeckillGoods();
            sg.setActivityId(1L);
            sg.setGoodsId(101L);
            sg.setSeckillPrice(new BigDecimal("7999"));
            sg.setSeckillStock(100);
            sg.setSalesCount(60);
            sg.setLimitPerUser(1);

            when(seckillActivityMapper.selectById(1L)).thenReturn(act);
            when(seckillGoodsMapper.selectList(any())).thenReturn(List.of(sg));
            when(goodsMapper.selectById(101L)).thenReturn(goods(101L, "iPhone", new BigDecimal("9999"), 50));

            AdminSeckillStatisticsResponse result = adminSeckillService.getActivityStatistics(1L);

            assertNotNull(result);
            assertEquals(1L, result.getActivityId());
            assertEquals(100, result.getTotalStock());
            assertEquals(60, result.getTotalSales());
            assertEquals(60.0, result.getSalesRate());
            assertNotNull(result.getGoodsStatistics());
            assertEquals(1, result.getGoodsStatistics().size());
            assertNotNull(result.getTimeDistribution());
        }

        @Test
        @DisplayName("活动不存在")
        void getActivityStatistics_NotFound() {
            when(seckillActivityMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> adminSeckillService.getActivityStatistics(999L));
            assertEquals(ResponseCodeEnum.SECKILL_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    private SeckillActivity activity(Long id, String name, LocalDateTime start, LocalDateTime end) {
        SeckillActivity act = new SeckillActivity();
        act.setId(id);
        act.setActivityName(name);
        act.setActivityImg("https://example.com/activity.jpg");
        act.setDescription("活动描述");
        act.setStartTime(start);
        act.setEndTime(end);
        act.setStatus(0);
        act.setCreateTime(LocalDateTime.now());
        act.setUpdateTime(LocalDateTime.now());
        return act;
    }

    private Goods goods(Long id, String name, BigDecimal price, int stock) {
        Goods g = new Goods();
        g.setId(id);
        g.setName(name);
        g.setPrice(price);
        g.setStock(stock);
        g.setCoverImage("https://example.com/goods.jpg");
        return g;
    }

    private AdminSeckillActivityRequest createActivityRequest(LocalDateTime start, LocalDateTime end) {
        AdminSeckillActivityRequest request = new AdminSeckillActivityRequest();
        request.setActivityName("测试秒杀活动");
        request.setDescription("测试描述");
        request.setActivityImg("https://example.com/activity.jpg");
        request.setStartTime(start);
        request.setEndTime(end);

        AdminSeckillGoodsItemRequest item = new AdminSeckillGoodsItemRequest();
        item.setGoodsId(101L);
        item.setSeckillPrice(new BigDecimal("7999"));
        item.setSeckillStock(20);
        item.setLimitPerUser(1);

        request.setGoodsList(List.of(item));
        return request;
    }
}
