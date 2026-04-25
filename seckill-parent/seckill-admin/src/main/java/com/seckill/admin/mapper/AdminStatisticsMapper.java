package com.seckill.admin.mapper;

import com.seckill.admin.dto.stats.DashboardStatsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端统计查询 Mapper。
 */
@Mapper
public interface AdminStatisticsMapper {

    @Select("SELECT COUNT(1) FROM t_user WHERE deleted = 0")
    long countTotalUsers();

    @Select("SELECT COUNT(1) FROM t_user WHERE deleted = 0 AND DATE(create_time) = CURDATE()")
    long countTodayNewUsers();

    @Select("SELECT COUNT(1) FROM t_order WHERE deleted = 0")
    long countTotalOrders();

    @Select("SELECT COUNT(1) FROM t_order WHERE deleted = 0 AND DATE(create_time) = CURDATE()")
    long countTodayOrders();

    @Select("""
            SELECT COALESCE(SUM(order_price * IFNULL(quantity, 1)), 0)
            FROM t_order
            WHERE deleted = 0
              AND status IN (2, 3, 4)
            """)
    BigDecimal sumTotalSales();

    @Select("""
            SELECT COALESCE(SUM(order_price * IFNULL(quantity, 1)), 0)
            FROM t_order
            WHERE deleted = 0
              AND status IN (2, 3, 4)
              AND DATE(create_time) = CURDATE()
            """)
    BigDecimal sumTodaySales();

    @Select("SELECT COUNT(1) FROM t_goods WHERE deleted = 0")
    long countTotalGoods();

    @Select("SELECT COUNT(1) FROM t_goods WHERE deleted = 0 AND status = 1")
    long countOnlineGoods();

    @Select("SELECT COUNT(1) FROM t_seckill_activity")
    long countTotalActivities();

    @Select("""
            SELECT COUNT(1) FROM t_seckill_activity
            WHERE start_time <= NOW() AND end_time >= NOW()
            """)
    long countOngoingActivities();

    @Select("""
            SELECT DATE(create_time) as date,
                   COALESCE(SUM(order_price * IFNULL(quantity, 1)), 0) as amount,
                   COUNT(1) as orderCount
            FROM t_order
            WHERE deleted = 0
              AND status IN (2, 3, 4)
              AND create_time >= #{startDate}
            GROUP BY DATE(create_time)
            ORDER BY date DESC
            LIMIT 7
            """)
    List<DashboardStatsResponse.SalesTrendItem> selectSalesTrend(@Param("startDate") LocalDateTime startDate);

    @Select("""
            SELECT o.goods_id as goodsId,
                   o.goods_name as goodsName,
                   SUM(IFNULL(o.quantity, 1)) as sales,
                   SUM(o.order_price * IFNULL(o.quantity, 1)) as amount
            FROM t_order o
            WHERE o.deleted = 0
              AND o.status IN (2, 3, 4)
            GROUP BY o.goods_id, o.goods_name
            ORDER BY sales DESC
            LIMIT 10
            """)
    List<DashboardStatsResponse.TopSellingProduct> selectTopSellingGoods();

    @Select("""
            SELECT o.order_no as orderNo,
                   u.username as username,
                   o.goods_name as goodsName,
                   o.order_price * IFNULL(o.quantity, 1) as totalAmount,
                   o.status as status,
                   CASE o.status
                       WHEN 1 THEN '待支付'
                       WHEN 2 THEN '已支付'
                       WHEN 3 THEN '已发货'
                       WHEN 4 THEN '已完成'
                       WHEN 5 THEN '已取消'
                       WHEN 6 THEN '已退款'
                       ELSE '初始化'
                   END as statusDesc,
                   DATE_FORMAT(o.create_time, '%Y-%m-%d %H:%i:%s') as createTime
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id AND u.deleted = 0
            WHERE o.deleted = 0
            ORDER BY o.create_time DESC
            LIMIT 10
            """)
    List<DashboardStatsResponse.RecentOrder> selectRecentOrders();

    @Select("""
            SELECT DATE(create_time) as date,
                   COALESCE(SUM(order_price * IFNULL(quantity, 1)), 0) as amount
            FROM t_order
            WHERE deleted = 0
              AND status IN (2, 3, 4)
              AND create_time >= #{startDate}
              AND create_time < #{endDate}
            GROUP BY DATE(create_time)
            ORDER BY date ASC
            """)
    List<SalesAmountItem> selectSalesAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Select("""
            SELECT DATE(create_time) as date,
                   COUNT(1) as orderCount
            FROM t_order
            WHERE deleted = 0
              AND status IN (2, 3, 4)
              AND create_time >= #{startDate}
              AND create_time < #{endDate}
            GROUP BY DATE(create_time)
            ORDER BY date ASC
            """)
    List<SalesCountItem> selectSalesCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Select("""
            SELECT o.goods_name as goodsName,
                   SUM(IFNULL(o.quantity, 1)) as sales
            FROM t_order o
            WHERE o.deleted = 0
              AND o.status IN (2, 3, 4)
            GROUP BY o.goods_id, o.goods_name
            ORDER BY sales DESC
            LIMIT 10
            """)
    List<TopProductItem> selectTopProducts();

    /**
     * 销售额数据项。
     */
    class SalesAmountItem {
        private String date;
        private BigDecimal amount;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /**
     * 订单数数据项。
     */
    class SalesCountItem {
        private String date;
        private Long orderCount;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    }

    /**
     * 热销商品数据项。
     */
    class TopProductItem {
        private String goodsName;
        private Long sales;

        public String getGoodsName() { return goodsName; }
        public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
        public Long getSales() { return sales; }
        public void setSales(Long sales) { this.sales = sales; }
    }
}
