package com.seckill.common.utils;

/**
 * Twitter Snowflake 算法实现
 * 用于生成全局唯一ID
 *
 * 结构：
 * 1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号 = 64位Long
 *
 * @author seckill
 */
public class SnowflakeIdWorker {

    // ==================== 常量定义 ====================

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 机器ID所占位数
     */
    private static final long WORKER_ID_BITS = 10L;

    /**
     * 序列号所占位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值（1023）
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 序列号最大值（4095）
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // ==================== 成员变量 ====================

    /**
     * 机器ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    // ==================== 单例模式 ====================

    private static volatile SnowflakeIdWorker instance;

    /**
     * 获取单例实例
     */
    public static SnowflakeIdWorker getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdWorker.class) {
                if (instance == null) {
                    // 默认机器ID为0，生产环境应从配置中心获取
                    instance = new SnowflakeIdWorker(0);
                }
            }
        }
        return instance;
    }

    /**
     * 获取指定机器ID的实例
     */
    public static SnowflakeIdWorker getInstance(long workerId) {
        if (instance == null) {
            synchronized (SnowflakeIdWorker.class) {
                if (instance == null) {
                    instance = new SnowflakeIdWorker(workerId);
                }
            }
        }
        return instance;
    }

    // ==================== 构造方法 ====================

    /**
     * 构造方法
     *
     * @param workerId 机器ID (0-1023)
     */
    private SnowflakeIdWorker(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("机器ID必须在 0-%d 之间", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }

    // ==================== 核心方法 ====================

    /**
     * 生成下一个ID（线程安全）
     *
     * @return 全局唯一ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 如果当前时间小于上次生成时间，说明时钟回拨，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("时钟回拨，拒绝生成ID。上次时间戳：%d，当前时间戳：%d",
                            lastTimestamp, timestamp));
        }

        // 如果是同一时间生成的，则进行序列号累加
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，重置序列号
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组合ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 生成订单号（带前缀）
     */
    public static String generateOrderNo() {
        return "ORD" + getInstance().nextId();
    }

    /**
     * 生成支付流水号（带前缀）
     */
    public static String generatePayNo() {
        return "PAY" + getInstance().nextId();
    }

}
