package com.seckill.common.constant;

/**
 * Redis Key 常量定义
 * 统一管理所有 Redis Key，避免硬编码
 *
 * @author seckill
 */
public class RedisKeyConstant {

    /**
     * Key 前缀
     */
    private static final String PREFIX = "seckill:";

    // ==================== 用户相关 ====================

    /**
     * 用户 Token 前缀
     * key: seckill:user:token:{token}
     * value: userId
     */
    public static final String USER_TOKEN = PREFIX + "user:token:";

    /**
     * 用户信息前缀
     * key: seckill:user:info:{userId}
     * value: User 对象
     */
    public static final String USER_INFO = PREFIX + "user:info:";

    /**
     * 用户登录失败次数前缀
     * key: seckill:user:login:fail:{username}
     * value: 失败次数
     */
    public static final String USER_LOGIN_FAIL = PREFIX + "user:login:fail:";

    // ==================== 商品相关 ====================

    /**
     * 商品信息前缀
     * key: seckill:goods:info:{goodsId}
     * value: Goods 对象
     */
    public static final String GOODS_INFO = PREFIX + "goods:info:";

    /**
     * 商品库存前缀
     * key: seckill:goods:stock:{goodsId}
     * value: 库存数量
     */
    public static final String GOODS_STOCK = PREFIX + "goods:stock:";

    // ==================== 秒杀相关 ====================

    /**
     * 秒杀活动信息前缀
     * key: seckill:activity:info:{activityId}
     * value: SeckillActivity 对象
     */
    public static final String SECKILL_ACTIVITY_INFO = PREFIX + "activity:info:";

    /**
     * 秒杀商品库存前缀
     * key: seckill:seckill:stock:{activityId}:{goodsId}
     * value: 秒杀库存数量
     */
    public static final String SECKILL_STOCK = PREFIX + "stock:";

    /**
     * 秒杀商品已售数量前缀
     * key: seckill:seckill:sold:{activityId}:{goodsId}
     * value: 已售数量
     */
    public static final String SECKILL_SOLD = PREFIX + "sold:";

    /**
     * 用户秒杀记录前缀（用于防重复秒杀）
     * key: seckill:seckill:record:{activityId}:{goodsId}:{userId}
     * value: 订单ID
     */
    public static final String SECKILL_RECORD = PREFIX + "record:";

    /**
     * 秒杀地址前缀（用于隐藏真实秒杀地址）
     * key: seckill:seckill:path:{userId}:{goodsId}
     * value: 加密后的秒杀路径
     */
    public static final String SECKILL_PATH = PREFIX + "path:";

    /**
     * 秒杀地址获取次数前缀
     * key: seckill:path:count:{userId}:{activityId}:{goodsId}
     * value: 获取次数
     */
    public static final String SECKILL_PATH_COUNT = PREFIX + "path:count:";

    /**
     * 秒杀验证码前缀
     * key: seckill:seckill:verify:{userId}:{goodsId}
     * value: 验证码
     */
    public static final String SECKILL_VERIFY = PREFIX + "verify:";

    /**
     * 秒杀限流计数器前缀
     * key: seckill:seckill:limit:{userId}
     * value: 访问次数
     */
    public static final String SECKILL_LIMIT = PREFIX + "limit:";

    /**
     * 已参与秒杀用户集合前缀
     * key: seckill:done:{activityId}:{goodsId}
     * value: 用户ID Set
     */
    public static final String SECKILL_DONE = PREFIX + "done:";

    /**
     * 秒杀结果缓存前缀
     * key: seckill:result:{recordId}
     * value: 秒杀结果对象
     */
    public static final String SECKILL_RESULT = PREFIX + "result:";

    // ==================== 支付相关 ====================

    /**
     * 支付状态缓存前缀
     * key: seckill:pay:status:{orderNo}
     * value: 支付状态对象
     */
    public static final String PAY_STATUS = PREFIX + "pay:status:";

    /**
     * 支付回调锁前缀
     * key: seckill:pay:callback:lock:{orderNo}
     * value: requestId
     */
    public static final String PAY_CALLBACK_LOCK = PREFIX + "pay:callback:lock:";

    // ==================== 订单相关 ====================

    /**
     * 订单信息前缀
     * key: seckill:order:info:{orderId}
     * value: Order 对象
     */
    public static final String ORDER_INFO = PREFIX + "order:info:";

    /**
     * 订单超时前缀（用于延迟队列）
     * key: seckill:order:timeout:{orderId}
     * value: 订单创建时间
     */
    public static final String ORDER_TIMEOUT = PREFIX + "order:timeout:";

    // ==================== 限流相关 ====================

    /**
     * 接口限流前缀
     * key: seckill:rate:limit:{uri}:{userId}
     * value: 访问次数
     */
    public static final String RATE_LIMIT = PREFIX + "rate:limit:";

    /**
     * 全局限流前缀
     * key: seckill:rate:global:{uri}
     * value: 访问次数
     */
    public static final String RATE_LIMIT_GLOBAL = PREFIX + "rate:global:";

    // ==================== 分布式锁相关 ====================

    /**
     * 分布式锁前缀
     * key: seckill:lock:{resource}
     * value: requestId
     */
    public static final String LOCK = PREFIX + "lock:";

    // ==================== 工具方法 ====================

    /**
     * 拼接 Key
     */
    public static String join(String prefix, Object... keys) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object key : keys) {
            sb.append(key.toString());
        }
        return sb.toString();
    }

}
