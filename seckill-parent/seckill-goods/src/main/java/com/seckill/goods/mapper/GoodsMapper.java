package com.seckill.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品表 Mapper。
 * <p>
 * 除了通用 CRUD 外，还提供一个原子扣减库存并增加销量的方法，
 * 用于后续秒杀或下单流程避免“先查后改”带来的并发问题。
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 扣减普通商品库存，同时累加销量。
     *
     * @param goodsId 商品 ID
     * @param quantity 扣减数量
     * @return 受影响行数，返回 1 代表扣减成功，0 代表库存不足或商品不可用
     */
    @Update("""
            UPDATE t_goods
            SET stock = stock - #{quantity},
                sales = sales + #{quantity},
                update_time = NOW()
            WHERE id = #{goodsId}
              AND deleted = 0
              AND stock >= #{quantity}
            """)
    int deductStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);

    /**
     * 回滚普通商品库存，同时回退销量。
     */
    @Update("""
            UPDATE t_goods
            SET stock = stock + #{quantity},
                sales = CASE
                    WHEN sales >= #{quantity} THEN sales - #{quantity}
                    ELSE 0
                END,
                update_time = NOW()
            WHERE id = #{goodsId}
              AND deleted = 0
            """)
    int rollbackStock(@Param("goodsId") Long goodsId, @Param("quantity") int quantity);
}
