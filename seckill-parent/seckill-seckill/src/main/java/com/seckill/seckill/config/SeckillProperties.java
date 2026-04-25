package com.seckill.seckill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "seckill.seckill")
public class SeckillProperties {

    private int rateLimitPerUser = 1;

    private int rateLimitGlobal = 5000;

    private int pathTtlSeconds = 300;

    private int pathMaxAcquireCount = 3;

    private int resultTtlSeconds = 86400;

    private int doneTtlSeconds = 86400;
}
