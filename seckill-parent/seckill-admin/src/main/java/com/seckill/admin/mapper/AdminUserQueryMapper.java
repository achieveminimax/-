package com.seckill.admin.mapper;

import com.seckill.admin.dto.user.AdminUserDetailResponse;
import com.seckill.admin.dto.user.AdminUserListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理端用户查询 Mapper。
 */
@Mapper
public interface AdminUserQueryMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM t_user u
            WHERE u.deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                     OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="status != null">
                AND u.status = #{status}
              </if>
            </script>
            """)
    long count(@Param("keyword") String keyword,
               @Param("status") Integer status);

    @Select("""
            <script>
            SELECT u.id AS userId,
                   u.username,
                   u.nickname,
                   CONCAT(LEFT(u.phone, 3), '****', RIGHT(u.phone, 4)) AS phone,
                   u.email,
                   u.status,
                   CASE u.status
                       WHEN 1 THEN '正常'
                       ELSE '禁用'
                   END AS statusDesc,
                   (SELECT COUNT(1) FROM t_order o WHERE o.user_id = u.id AND o.deleted = 0) AS orderCount,
                   (SELECT COALESCE(SUM(o.order_price * IFNULL(o.quantity, 1)), 0)
                    FROM t_order o
                    WHERE o.user_id = u.id
                      AND o.status IN (2, 3, 4)
                      AND o.deleted = 0) AS totalAmount,
                   u.lock_time AS lastLoginTime,
                   u.create_time AS createTime
            FROM t_user u
            WHERE u.deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                     OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="status != null">
                AND u.status = #{status}
              </if>
            ORDER BY u.id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AdminUserListResponse> selectPage(@Param("keyword") String keyword,
                                           @Param("status") Integer status,
                                           @Param("offset") long offset,
                                           @Param("size") long size);

    @Select("""
            SELECT u.id AS userId,
                   u.username,
                   u.nickname,
                   CONCAT(LEFT(u.phone, 3), '****', RIGHT(u.phone, 4)) AS phone,
                   u.email,
                   u.avatar,
                   u.gender,
                   u.birthday,
                   u.status,
                   CASE u.status
                       WHEN 1 THEN '正常'
                       ELSE '禁用'
                   END AS statusDesc,
                   (SELECT COUNT(1) FROM t_order o WHERE o.user_id = u.id AND o.deleted = 0) AS orderCount,
                   (SELECT COALESCE(SUM(o.order_price * IFNULL(o.quantity, 1)), 0)
                    FROM t_order o
                    WHERE o.user_id = u.id
                      AND o.status IN (2, 3, 4)
                      AND o.deleted = 0) AS totalAmount,
                   u.lock_time AS lastLoginTime,
                   u.create_time AS createTime,
                   u.update_time AS updateTime
            FROM t_user u
            WHERE u.id = #{userId}
              AND u.deleted = 0
            LIMIT 1
            """)
    AdminUserDetailResponse selectDetail(@Param("userId") Long userId);
}
