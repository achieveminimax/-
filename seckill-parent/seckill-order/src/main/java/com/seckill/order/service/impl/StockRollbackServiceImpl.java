package com.seckill.order.service.impl;

import com.seckill.goods.mapper.GoodsMapper;
import com.seckill.goods.mapper.SeckillGoodsMapper;
import com.seckill.infrastructure.utils.RedisOperations;
import com.seckill.order.service.StockRollbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class StockRollbackServiceImpl implements StockRollbackService {

    private final RedisOperations redisOperations;
    private final GoodsMapper goodsMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;

    private static final long DONE_TTL_SECONDS = 86400;

    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rollback_stock.lua")));
        script.setResultType(Long.class);
        ROLLBACK_SCRIPT = script;
    }

    @Autowired
    public StockRollbackServiceImpl(RedisOperations redisOperations,
                                    GoodsMapper goodsMapper,
                                    SeckillGoodsMapper seckillGoodsMapper) {
        this.redisOperations = redisOperations;
        this.goodsMapper = goodsMapper;
        this.seckillGoodsMapper = seckillGoodsMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackStock(Long activityId, Long goodsId, Long userId, int quantity) {
        if (activityId == null || goodsId == null || userId == null) {
            log.warn("库存回滚参数无效, activityId={}, goodsId={}, userId={}", activityId, goodsId, userId);
            return;
        }

        log.info("开始库存回滚, activityId={}, goodsId={}, userId={}, quantity={}",
                activityId, goodsId, userId, quantity);

        rollbackRedisStock(activityId, goodsId, userId, quantity);

        rollbackDbStock(goodsId, quantity);

        rollbackSeckillSales(activityId, goodsId, quantity);

        log.info("库存回滚完成, activityId={}, goodsId={}, userId={}", activityId, goodsId, userId);
    }

    private void rollbackRedisStock(Long activityId, Long goodsId, Long userId, int quantity) {
        try {
            String stockKey = "seckill:stock:" + activityId + ":" + goodsId;
            String doneKey = "seckill:done:" + activityId + ":" + goodsId;
            List<String> keys = Arrays.asList(stockKey, doneKey);
            Long result = redisOperations.execute(ROLLBACK_SCRIPT, keys,
                    String.valueOf(quantity), String.valueOf(userId), String.valueOf(DONE_TTL_SECONDS));
            log.info("Redis 库存回滚成功(原子操作), stockKey={}, doneKey={}, result={}", stockKey, doneKey, result);
        } catch (Exception e) {
            log.error("Redis 库存回滚失败, activityId={}, goodsId={}, quantity={}",
                    activityId, goodsId, quantity, e);
            throw e;
        }
    }

    private void rollbackDbStock(Long goodsId, int quantity) {
        try {
            int affected = goodsMapper.rollbackStock(goodsId, quantity);
            log.info("数据库商品库存回滚成功, goodsId={}, quantity={}, affected={}", goodsId, quantity, affected);
        } catch (Exception e) {
            log.error("数据库商品库存回滚失败, goodsId={}, quantity={}", goodsId, quantity, e);
            throw e;
        }
    }

    private void rollbackSeckillSales(Long activityId, Long goodsId, int quantity) {
        try {
            int affected = seckillGoodsMapper.rollbackSalesCount(activityId, goodsId, quantity);
            log.info("数据库秒杀销量回滚成功, activityId={}, goodsId={}, quantity={}, affected={}",
                    activityId, goodsId, quantity, affected);
        } catch (Exception e) {
            log.error("数据库秒杀销量回滚失败, activityId={}, goodsId={}, quantity={}",
                    activityId, goodsId, quantity, e);
            throw e;
        }
    }
}
