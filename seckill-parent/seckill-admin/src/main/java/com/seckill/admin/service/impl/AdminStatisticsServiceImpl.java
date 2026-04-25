package com.seckill.admin.service.impl;

import com.seckill.admin.dto.stats.DashboardStatsResponse;
import com.seckill.admin.dto.stats.SalesStatsResponse;
import com.seckill.admin.dto.stats.TopProductsResponse;
import com.seckill.admin.mapper.AdminStatisticsMapper;
import com.seckill.admin.service.AdminStatisticsService;
import com.seckill.infrastructure.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理端统计服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final String DASHBOARD_CACHE_KEY = "admin:dashboard";
    private static final long DASHBOARD_CACHE_TTL = 300; // 5 分钟

    private final AdminStatisticsMapper adminStatisticsMapper;
    private final RedisUtils redisUtils;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        // 尝试从缓存获取
        DashboardStatsResponse cached = redisUtils.get(DASHBOARD_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        DashboardStatsResponse response = new DashboardStatsResponse();

        // 概览统计
        DashboardStatsResponse.OverviewStats overview = new DashboardStatsResponse.OverviewStats();
        overview.setTotalUsers(adminStatisticsMapper.countTotalUsers());
        overview.setTodayNewUsers(adminStatisticsMapper.countTodayNewUsers());
        overview.setTotalOrders(adminStatisticsMapper.countTotalOrders());
        overview.setTodayOrders(adminStatisticsMapper.countTodayOrders());
        overview.setTotalSales(adminStatisticsMapper.sumTotalSales());
        overview.setTodaySales(adminStatisticsMapper.sumTodaySales());
        overview.setTotalGoods(adminStatisticsMapper.countTotalGoods());
        overview.setOnlineGoods(adminStatisticsMapper.countOnlineGoods());
        overview.setTotalSeckillActivities(adminStatisticsMapper.countTotalActivities());
        overview.setOngoingActivities(adminStatisticsMapper.countOngoingActivities());
        response.setOverview(overview);

        // 销售趋势（最近 7 天）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<DashboardStatsResponse.SalesTrendItem> salesTrend = adminStatisticsMapper.selectSalesTrend(sevenDaysAgo);
        // 补齐缺失的日期
        response.setSalesTrend(fillMissingDates(salesTrend, 7));

        // 热销商品 Top 10
        response.setTopSellingGoods(adminStatisticsMapper.selectTopSellingGoods());

        // 最近订单
        response.setRecentOrders(adminStatisticsMapper.selectRecentOrders());

        // 缓存结果
        redisUtils.set(DASHBOARD_CACHE_KEY, response, DASHBOARD_CACHE_TTL, TimeUnit.SECONDS);

        return response;
    }

    @Override
    public SalesStatsResponse getSalesStats(Integer days) {
        SalesStatsResponse response = new SalesStatsResponse();

        // 计算日期范围
        LocalDateTime endDate = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startDate = endDate.minusDays(days);

        // 查询销售额和订单数
        List<AdminStatisticsMapper.SalesAmountItem> amountItems = 
                adminStatisticsMapper.selectSalesAmountByDateRange(startDate, endDate);
        List<AdminStatisticsMapper.SalesCountItem> countItems = 
                adminStatisticsMapper.selectSalesCountByDateRange(startDate, endDate);

        // 构建日期列表
        List<String> dateList = new ArrayList<>();
        List<BigDecimal> amountList = new ArrayList<>();
        List<Long> countList = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i).toLocalDate();
            String dateStr = date.format(formatter);
            dateList.add(dateStr);

            // 查找对应日期的数据
            String dateKey = date.toString();
            BigDecimal amount = amountItems.stream()
                    .filter(item -> item.getDate().equals(dateKey))
                    .findFirst()
                    .map(AdminStatisticsMapper.SalesAmountItem::getAmount)
                    .orElse(BigDecimal.ZERO);
            amountList.add(amount);

            Long count = countItems.stream()
                    .filter(item -> item.getDate().equals(dateKey))
                    .findFirst()
                    .map(AdminStatisticsMapper.SalesCountItem::getOrderCount)
                    .orElse(0L);
            countList.add(count);
        }

        response.setXAxis(dateList);

        // 构建 series
        List<SalesStatsResponse.SeriesItem> series = new ArrayList<>();
        
        SalesStatsResponse.SeriesItem amountSeries = new SalesStatsResponse.SeriesItem();
        amountSeries.setName("销售额");
        amountSeries.setType("line");
        amountSeries.setData(amountList);
        series.add(amountSeries);

        SalesStatsResponse.SeriesItem countSeries = new SalesStatsResponse.SeriesItem();
        countSeries.setName("订单数");
        countSeries.setType("bar");
        // 将 Long 转换为 BigDecimal
        countSeries.setData(countList.stream().map(BigDecimal::valueOf).collect(Collectors.toList()));
        series.add(countSeries);

        response.setSeries(series);

        // 构建 legend
        SalesStatsResponse.Legend legend = new SalesStatsResponse.Legend();
        legend.setData(List.of("销售额", "订单数"));
        response.setLegend(legend);

        return response;
    }

    @Override
    public TopProductsResponse getTopProducts() {
        TopProductsResponse response = new TopProductsResponse();

        List<AdminStatisticsMapper.TopProductItem> topProducts = adminStatisticsMapper.selectTopProducts();

        List<String> names = topProducts.stream()
                .map(AdminStatisticsMapper.TopProductItem::getGoodsName)
                .collect(Collectors.toList());
        List<Long> sales = topProducts.stream()
                .map(AdminStatisticsMapper.TopProductItem::getSales)
                .collect(Collectors.toList());

        response.setXAxis(names);

        TopProductsResponse.SeriesItem series = new TopProductsResponse.SeriesItem();
        series.setName("销量");
        series.setType("bar");
        series.setData(sales);

        response.setSeries(List.of(series));

        return response;
    }

    /**
     * 补齐缺失的日期数据。
     */
    private List<DashboardStatsResponse.SalesTrendItem> fillMissingDates(
            List<DashboardStatsResponse.SalesTrendItem> items, int days) {
        
        List<DashboardStatsResponse.SalesTrendItem> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.toString();

            DashboardStatsResponse.SalesTrendItem item = items.stream()
                    .filter(it -> it.getDate().equals(dateStr))
                    .findFirst()
                    .orElse(null);

            if (item == null) {
                item = new DashboardStatsResponse.SalesTrendItem();
                item.setDate(dateStr);
                item.setAmount(BigDecimal.ZERO);
                item.setOrderCount(0L);
            }

            result.add(item);
        }

        return result;
    }
}
