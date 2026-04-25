package com.seckill.infrastructure.utils;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface RedisOperations {

    Long execute(DefaultRedisScript<Long> script, List<String> keys, String... args);

    Long increment(String key);

    Boolean expire(String key, long timeout, TimeUnit unit);

    Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit);

    Boolean delete(String key);

    String get(String key);

    void set(String key, String value, long timeout, TimeUnit unit);
}
