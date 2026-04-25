package com.seckill.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND deleted = 0 LIMIT 1")
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            SELECT *
            FROM t_order
            WHERE order_no = #{orderNo}
              AND user_id = #{userId}
              AND deleted = 0
            LIMIT 1
            """)
    Order selectOwnedOrder(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    @Update("""
            UPDATE t_order
            SET status = #{targetStatus},
                cancel_time = NOW(),
                cancel_reason = #{cancelReason},
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND user_id = #{userId}
              AND status = #{currentStatus}
              AND deleted = 0
            """)
    int cancelOrder(@Param("orderNo") String orderNo,
                    @Param("userId") Long userId,
                    @Param("currentStatus") Integer currentStatus,
                    @Param("targetStatus") Integer targetStatus,
                    @Param("cancelReason") String cancelReason);

    @Update("""
            UPDATE t_order
            SET status = #{targetStatus},
                pay_type = #{payType},
                pay_time = #{payTime},
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND status = #{currentStatus}
              AND deleted = 0
            """)
    int markPaid(@Param("orderNo") String orderNo,
                 @Param("currentStatus") Integer currentStatus,
                 @Param("targetStatus") Integer targetStatus,
                 @Param("payType") Integer payType,
                 @Param("payTime") java.time.LocalDateTime payTime);
}
