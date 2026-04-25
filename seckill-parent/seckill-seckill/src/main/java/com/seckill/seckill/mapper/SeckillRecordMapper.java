package com.seckill.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.seckill.entity.SeckillRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillRecordMapper extends BaseMapper<SeckillRecord> {

    @Select("""
            SELECT *
            FROM t_seckill_record
            WHERE id = #{recordId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    SeckillRecord selectByIdAndUserId(@Param("recordId") Long recordId, @Param("userId") Long userId);

    @Update("""
            UPDATE t_seckill_record
            SET order_id = #{orderId},
                status = 1,
                fail_reason = NULL,
                finish_time = NOW()
            WHERE id = #{recordId}
              AND status = 0
            """)
    int markSuccess(@Param("recordId") Long recordId, @Param("orderId") Long orderId);

    @Update("""
            UPDATE t_seckill_record
            SET status = 2,
                fail_reason = #{failReason},
                finish_time = NOW()
            WHERE id = #{recordId}
              AND status = 0
            """)
    int markFailed(@Param("recordId") Long recordId, @Param("failReason") String failReason);
}
