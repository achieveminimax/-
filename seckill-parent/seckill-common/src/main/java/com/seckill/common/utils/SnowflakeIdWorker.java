package com.seckill.common.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 雪花算法 ID 生成器
 * <p>
 * 结构：
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 * 1位标识位 + 41位时间戳 + 5位数据中心ID + 5位机器ID + 12位序列号
 */
public class SnowflakeIdWorker {

    // ============================== 常量 ==============================

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     */
    private final long START_TIMESTAMP = 1704067200000L;

    /**
     * 数据中心ID占用的位数
     */
    private final long DATA_CENTER_ID_BITS = 5L;

    /**
     * 机器ID占用的位数
     */
    private final long WORKER_ID_BITS = 5L;

    /**
     * 序列号占用的位数
     */
    private final long SEQUENCE_BITS = 12L;

    /**
     * 最大数据中心ID（31）
     */
    private final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /**
     * 最大机器ID（31）
     */
    private final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 最大序列号（4095）
     */
    private final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数
     */
    private final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID左移位数
     */
    private final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // ============================== 成员变量 ==============================

    /**
     * 数据中心ID
     */
    private final long dataCenterId;

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

    // ============================== 单例 ==============================

    private static volatile SnowflakeIdWorker instance;

    /**
     * 获取单例实例
     */
    public static SnowflakeIdWorker getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdWorker.class) {
                if (instance == null) {
                    instance = new SnowflakeIdWorker();
                }
            }
        }
        return instance;
    }

    /**
     * 获取指定数据中心ID和机器ID的实例
     */
    public static SnowflakeIdWorker getInstance(long dataCenterId, long workerId) {
        return new SnowflakeIdWorker(dataCenterId, workerId);
    }

    // ============================== 构造方法 ==============================

    /**
     * 默认构造方法（自动生成数据中心ID和机器ID）
     */
    private SnowflakeIdWorker() {
        this.dataCenterId = generateDataCenterId();
        this.workerId = generateWorkerId();
    }

    /**
     * 构造方法
     *
     * @param dataCenterId 数据中心ID (0-31)
     * @param workerId     机器ID (0-31)
     */
    private SnowflakeIdWorker(long dataCenterId, long workerId) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID必须在0-31之间");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("机器ID必须在0-31之间");
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }

    // ============================== 核心方法 ==============================

    /**
     * 生成下一个ID（线程安全）
     *
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        // 如果当前时间小于上次生成ID的时间，说明系统时钟回退
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("系统时钟回退，拒绝生成ID");
        }

        // 如果是同一时间生成的，则进行序列号累加
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = getNextTimestamp(lastTimestamp);
            }
        } else {
            // 时间戳改变，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组合ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成订单号（带日期前缀）
     *
     * @return 订单号
     */
    public String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long id = nextId();
        return "SEK" + dateStr + String.format("%012d", id % 1000000000000L);
    }

    /**
     * 生成支付流水号
     *
     * @return 支付流水号
     */
    public String generatePayNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long id = nextId();
        return "PAY" + dateStr + String.format("%012d", id % 1000000000000L);
    }

    // ============================== 私有方法 ==============================

    /**
     * 获取当前时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取下一个时间戳
     */
    private long getNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 生成数据中心ID（基于IP地址）
     */
    private long generateDataCenterId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String hostAddress = ip.getHostAddress();
            // 取IP最后一段对32取模
            String[] parts = hostAddress.split("\\.");
            if (parts.length == 4) {
                return Long.parseLong(parts[3]) % (MAX_DATA_CENTER_ID + 1);
            }
        } catch (UnknownHostException e) {
            // 使用随机数
        }
        return new Random().nextInt((int) (MAX_DATA_CENTER_ID + 1));
    }

    /**
     * 生成机器ID（基于进程ID）
     */
    private long generateWorkerId() {
        try {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            // 进程ID对32取模
            if (processName.contains("@")) {
                return Long.parseLong(processName.split("@")[0]) % (MAX_WORKER_ID + 1);
            }
        } catch (Exception e) {
            // 使用随机数
        }
        return new Random().nextInt((int) (MAX_WORKER_ID + 1));
    }
}
