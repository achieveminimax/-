package com.seckill.admin.service;

import com.seckill.admin.dto.stats.DashboardStatsResponse;
import com.seckill.admin.dto.stats.SalesStatsResponse;
import com.seckill.admin.dto.stats.TopProductsResponse;
import com.seckill.admin.mapper.AdminStatisticsMapper;
import com.seckill.admin.service.impl.AdminStatisticsServiceImpl;
import com.seckill.infrastructure.utils.RedisUtils;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatisticsService 单元测试")
class AdminStatisticsServiceImplUnitTest {

    @Mock
    private AdminStatisticsMapper adminStatisticsMapper;

    @Mock
    private RedisUtils redisUtils;

    private AdminStatisticsServiceImpl adminStatisticsService;

    @BeforeEach
    void setUp() {
        adminStatisticsService = new AdminStatisticsServiceImpl(adminStatisticsMapper, redisUtils);
    }

    @Nested
    @DisplayName("getDashboardStats 测试")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("缓存命中直接返回")
        void getDashboardStats_Success_CacheHit() {
            DashboardStatsResponse cached = new DashboardStatsResponse();
            when(redisUtils.get("admin:dashboard")).thenReturn(cached);

            DashboardStatsResponse result = adminStatisticsService.getDashboardStats();

            assertNotNull(result);
            verify(adminStatisticsMapper, never()).countTotalUsers();
        }

        @Test
        @DisplayName("缓存未命中查库并缓存")
        void getDashboardStats_Success_NoCache() {
            when(redisUtils.get("admin:dashboard")).thenReturn(null);
            when(adminStatisticsMapper.countTotalUsers()).thenReturn(1000L);
            when(adminStatisticsMapper.countTodayNewUsers()).thenReturn(10L);
            when(adminStatisticsMapper.countTotalOrders()).thenReturn(500L);
            when(adminStatisticsMapper.countTodayOrders()).thenReturn(5L);
            when(adminStatisticsMapper.sumTotalSales()).thenReturn(new BigDecimal("99999.00"));
            when(adminStatisticsMapper.sumTodaySales()).thenReturn(new BigDecimal("500.00"));
            when(adminStatisticsMapper.countTotalGoods()).thenReturn(200L);
            when(adminStatisticsMapper.countOnlineGoods()).thenReturn(150L);
            when(adminStatisticsMapper.countTotalActivities()).thenReturn(20L);
            when(adminStatisticsMapper.countOngoingActivities()).thenReturn(3L);
            when(adminStatisticsMapper.selectSalesTrend(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
            when(adminStatisticsMapper.selectTopSellingGoods()).thenReturn(Collections.emptyList());
            when(adminStatisticsMapper.selectRecentOrders()).thenReturn(Collections.emptyList());

            DashboardStatsResponse result = adminStatisticsService.getDashboardStats();

            assertNotNull(result);
            assertNotNull(result.getOverview());
            assertEquals(1000L, result.getOverview().getTotalUsers());
            assertEquals(10L, result.getOverview().getTodayNewUsers());
            assertEquals(7, result.getSalesTrend().size());
            verify(redisUtils).set(eq("admin:dashboard"), eq(result), eq(300L), eq(TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("getSalesStats 测试")
    class GetSalesStatsTests {

        @Test
        @DisplayName("7天销售统计")
        void getSalesStats_Success() {
            when(adminStatisticsMapper.selectSalesAmountByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());
            when(adminStatisticsMapper.selectSalesCountByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            SalesStatsResponse result = adminStatisticsService.getSalesStats(7);

            assertNotNull(result);
            assertEquals(7, result.getXAxis().size());
            assertEquals(2, result.getSeries().size());
            assertEquals("销售额", result.getSeries().get(0).getName());
            assertEquals("订单数", result.getSeries().get(1).getName());
            assertNotNull(result.getLegend());
            assertEquals(2, result.getLegend().getData().size());
        }
    }

    @Nested
    @DisplayName("getTopProducts 测试")
    class GetTopProductsTests {

        @Test
        @DisplayName("热销商品查询")
        void getTopProducts_Success() {
            AdminStatisticsMapper.TopProductItem item = new AdminStatisticsMapper.TopProductItem();
            item.setGoodsName("iPhone 16 Pro Max");
            item.setSales(500L);
            when(adminStatisticsMapper.selectTopProducts()).thenReturn(java.util.List.of(item));

            TopProductsResponse result = adminStatisticsService.getTopProducts();

            assertNotNull(result);
            assertEquals(1, result.getXAxis().size());
            assertEquals("iPhone 16 Pro Max", result.getXAxis().getFirst());
            assertEquals(1, result.getSeries().size());
            assertEquals("销量", result.getSeries().getFirst().getName());
        }
    }
}
