package com.seckill.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.goods.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀商品关联表 Mapper。
 * <p>
 * 该表维护“活动中的商品”这一层关系，包含秒杀价、秒杀库存、限购数和已售数。
 */
@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 增加秒杀商品已售数量。
     *
     * @param activityId 活动 ID
     * @param goodsId 商品 ID
     * @param quantity 增加数量
     * @return 受影响行数，返回 1 代表增加成功，0 代表会超过秒杀库存上限
     */
    @Update("""
            UPDATE t_seckill_goods
            SET sales_count = sales_count + #{quantity},
                update_time = NOW()
            WHERE activity_id = #{activityId}
              AND goods_id = #{goodsId}
              AND sales_count + #{quantity} <= seckill_stock
            """)
    int incrementSalesCount(@Param("activityId") Long activityId,
                            @Param("goodsId") Long goodsId,
                            @Param("quantity") int quantity);

    /**
     * 回退秒杀商品销量。
     */
    @Update("""
            UPDATE t_seckill_goods
            SET sales_count = CASE
                    WHEN sales_count >= #{quantity} THEN sales_count - #{quantity}
                    ELSE 0
                END,
                update_time = NOW()
            WHERE activity_id = #{activityId}
              AND goods_id = #{goodsId}
            """)
    int rollbackSalesCount(@Param("activityId") Long activityId,
                           @Param("goodsId") Long goodsId,
                           @Param("quantity") int quantity);
}
