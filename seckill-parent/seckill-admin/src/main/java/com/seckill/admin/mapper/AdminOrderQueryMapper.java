package com.seckill.admin.mapper;

import com.seckill.admin.dto.order.AdminOrderDetailResponse;
import com.seckill.admin.dto.order.AdminOrderListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端订单查询 Mapper。
 */
@Mapper
public interface AdminOrderQueryMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id AND u.deleted = 0
            WHERE o.deleted = 0
              <if test="orderNo != null and orderNo != ''">
                AND o.order_no = #{orderNo}
              </if>
              <if test="userId != null">
                AND o.user_id = #{userId}
              </if>
              <if test="status != null and status != 0">
                AND o.status = #{status}
              </if>
              <if test="startTime != null">
                AND o.create_time <![CDATA[>=]]> #{startTime}
              </if>
              <if test="endTime != null">
                AND o.create_time <![CDATA[<=]]> #{endTime}
              </if>
            </script>
            """)
    long count(@Param("orderNo") String orderNo,
               @Param("userId") Long userId,
               @Param("status") Integer status,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT o.order_no AS orderNo,
                   o.user_id AS userId,
                   u.username AS username,
                   o.goods_id AS goodsId,
                   o.goods_name AS goodsName,
                   o.goods_image AS goodsImg,
                   o.order_price * IFNULL(o.quantity, 1) AS totalAmount,
                   o.status AS status,
                   CASE o.status
                       WHEN 1 THEN '待支付'
                       WHEN 2 THEN '已支付'
                       WHEN 3 THEN '已发货'
                       WHEN 4 THEN '已完成'
                       WHEN 5 THEN '已取消'
                       WHEN 6 THEN '已退款'
                       ELSE '初始化'
                   END AS statusDesc,
                   o.create_time AS createTime,
                   o.pay_time AS payTime
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id AND u.deleted = 0
            WHERE o.deleted = 0
              <if test="orderNo != null and orderNo != ''">
                AND o.order_no = #{orderNo}
              </if>
              <if test="userId != null">
                AND o.user_id = #{userId}
              </if>
              <if test="status != null and status != 0">
                AND o.status = #{status}
              </if>
              <if test="startTime != null">
                AND o.create_time <![CDATA[>=]]> #{startTime}
              </if>
              <if test="endTime != null">
                AND o.create_time <![CDATA[<=]]> #{endTime}
              </if>
            ORDER BY o.create_time DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminOrderListResponse> selectPage(@Param("orderNo") String orderNo,
                                            @Param("userId") Long userId,
                                            @Param("status") Integer status,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("offset") long offset,
                                            @Param("size") long size);

    @Select("""
            SELECT o.order_no AS orderNo,
                   o.user_id AS userId,
                   u.username AS username,
                   o.activity_id AS activityId,
                   o.goods_id AS goodsId,
                   o.goods_name AS goodsName,
                   o.goods_image AS goodsImg,
                   o.order_price * IFNULL(o.quantity, 1) AS totalAmount,
                   o.quantity AS quantity,
                   o.status AS status,
                   CASE o.status
                       WHEN 1 THEN '待支付'
                       WHEN 2 THEN '已支付'
                       WHEN 3 THEN '已发货'
                       WHEN 4 THEN '已完成'
                       WHEN 5 THEN '已取消'
                       WHEN 6 THEN '已退款'
                       ELSE '初始化'
                   END AS statusDesc,
                   o.pay_type AS payType,
                   o.pay_time AS payTime,
                   o.receiver_name AS receiverName,
                   o.receiver_phone AS receiverPhone,
                   o.receiver_address AS receiverAddress,
                   o.express_company AS expressCompany,
                   o.express_no AS expressNo,
                   o.ship_time AS shipTime,
                   o.create_time AS createTime,
                   o.update_time AS updateTime
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id AND u.deleted = 0
            WHERE o.order_no = #{orderNo}
              AND o.deleted = 0
            LIMIT 1
            """)
    AdminOrderDetailResponse selectDetail(@Param("orderNo") String orderNo);
}
