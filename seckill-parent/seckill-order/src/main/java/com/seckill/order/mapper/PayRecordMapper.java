package com.seckill.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.order.entity.PayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 支付记录 Mapper
 */
@Mapper
public interface PayRecordMapper extends BaseMapper<PayRecord> {

    /**
     * 根据支付流水号查询
     */
    @Select("SELECT * FROM t_pay_record WHERE pay_no = #{payNo} LIMIT 1")
    PayRecord selectByPayNo(@Param("payNo") String payNo);

    /**
     * 根据订单编号查询
     */
    @Select("SELECT * FROM t_pay_record WHERE order_no = #{orderNo} ORDER BY create_time DESC LIMIT 1")
    PayRecord selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新支付状态
     */
    @Update("""
            UPDATE t_pay_record
            SET status = #{status},
                trade_no = #{tradeNo},
                pay_time = #{payTime},
                update_time = NOW()
            WHERE pay_no = #{payNo}
              AND status = 0
            """)
    int updateStatus(@Param("payNo") String payNo,
                     @Param("status") Integer status,
                     @Param("tradeNo") String tradeNo,
                     @Param("payTime") LocalDateTime payTime);
}
