package com.seckill.common.config;

import com.seckill.common.utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公共模块配置类
 */
@Configuration
public class CommonConfig {

    /**
     * 雪花算法 ID 生成器
     */
    @Bean
    public SnowflakeIdWorker snowflakeIdWorker() {
        return SnowflakeIdWorker.getInstance();
    }
}
