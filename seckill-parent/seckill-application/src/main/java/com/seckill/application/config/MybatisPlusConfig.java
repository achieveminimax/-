package com.seckill.application.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 扫描所有模块的 Mapper 接口
 */
@Configuration
@MapperScan("com.seckill.**.mapper")
public class MybatisPlusConfig {
}
