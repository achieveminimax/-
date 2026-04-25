package com.seckill.infrastructure.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 封装常用的 Redis 操作
 *
 * @author seckill
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== Key 操作 ====================

    /**
     * 删除 key
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除 key
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 按模式批量删除 key（使用 SCAN 替代 KEYS，避免阻塞 Redis）
     */
    public Long deleteByPattern(String pattern) {
        Set<String> keys = scanKeys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        return delete(keys);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 使用 SCAN 命令迭代扫描匹配的 key（替代 KEYS 命令，避免阻塞 Redis）
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> result = new java.util.HashSet<>();
        try {
            var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern)
                    .count(200)
                    .build());
            if (cursor != null) {
                cursor.forEachRemaining(key -> result.add((String) key));
                cursor.close();
            }
        } catch (Exception e) {
            log.warn("SCAN 命令执行失败, pattern={}, 降级为 KEYS 命令", pattern, e);
            Set<String> fallback = redisTemplate.keys(pattern);
            if (fallback != null) {
                result.addAll(fallback);
            }
        }
        return result;
    }

    /**
     * 设置 key 过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取 key 过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ==================== String 操作 ====================

    /**
     * 设置 String 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 String 值（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取 String 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 原子递增
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子递减
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量设置 Hash 值
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取 Hash 值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取所有 Hash 值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     */
    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 判断 Hash 字段是否存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * Hash 字段递增
     */
    public Long hIncrement(String key, String hashKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }

    // ==================== List 操作 ====================

    /**
     * 从左侧推入 List
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从右侧推入 List
     */
    public Long rPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 从左侧弹出 List
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从右侧弹出 List
     */
    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取 List 范围
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取 List 长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== Set 操作 ====================

    /**
     * 添加 Set 成员
     */
    public Long sAdd(String key, Object... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    /**
     * 获取 Set 所有成员
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断是否为 Set 成员
     */
    public Boolean sIsMember(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 获取 Set 大小
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 删除 Set 成员
     */
    public Long sRemove(String key, Object... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    // ==================== ZSet 操作 ====================

    /**
     * 添加 ZSet 成员
     */
    public Boolean zAdd(String key, Object member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 获取 ZSet 范围（按分数从低到高）
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取 ZSet 大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 删除 ZSet 成员
     */
    public Long zRemove(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    // ==================== Pipeline 批量操作 ====================

    /**
     * Pipeline 批量设置 String 值（带过期时间），减少网络往返
     */
    public void pipelineSet(Map<String, Object> keyValueMap, long timeout, TimeUnit unit) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }
        redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            long timeoutSeconds = unit.toSeconds(timeout);
            org.springframework.data.redis.serializer.RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();
            org.springframework.data.redis.serializer.RedisSerializer<Object> valueSerializer = (org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer();
            for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
                byte[] keyBytes = stringSerializer.serialize(entry.getKey());
                byte[] valueBytes = valueSerializer.serialize(entry.getValue());
                connection.stringCommands().set(keyBytes, valueBytes,
                        org.springframework.data.redis.core.types.Expiration.seconds(timeoutSeconds),
                        org.springframework.data.redis.connection.RedisStringCommands.SetOption.UPSERT);
            }
            return null;
        });
    }

    /**
     * Pipeline 批量删除 key，减少网络往返
     */
    public Long pipelineDelete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        org.springframework.data.redis.serializer.RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            for (String key : keys) {
                connection.keyCommands().del(stringSerializer.serialize(key));
            }
            return null;
        });
        return (long) results.size();
    }

    /**
     * Pipeline 批量获取 String 值，减少网络往返
     */
    public List<Object> pipelineGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        org.springframework.data.redis.serializer.RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();
        return redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
            for (String key : keys) {
                connection.stringCommands().get(stringSerializer.serialize(key));
            }
            return null;
        });
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey   锁的 key
     * @param requestId 请求标识（用于释放锁时校验）
     * @param expireTime 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, int expireTime) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private static final org.springframework.data.redis.core.script.RedisScript<Long> RELEASE_LOCK_SCRIPT =
            org.springframework.data.redis.core.script.RedisScript.of(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);

    /**
     * 释放分布式锁（使用 Lua 脚本保证原子性，避免竞态条件）
     *
     * @param lockKey   锁的 key
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String requestId) {
        Long result = redisTemplate.execute(RELEASE_LOCK_SCRIPT,
                java.util.Collections.singletonList(lockKey), requestId);
        return result != null && result > 0;
    }

}
