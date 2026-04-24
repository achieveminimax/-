package com.seckill.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API 文档配置类
 *
 * @author seckill
 */
@Configuration
public class Knife4jConfig {

    /**
     * 配置 OpenAPI 基本信息
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("电商秒杀系统 API 文档")
                        .description("基于 Spring Boot 3.x + MyBatis-Plus + Redis + RabbitMQ 的高并发秒杀系统")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Seckill Team")
                                .email("seckill@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    /**
     * 用户模块 API 分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户模块")
                .pathsToMatch("/api/user/**")
                .build();
    }

    /**
     * 商品模块 API 分组
     */
    @Bean
    public GroupedOpenApi goodsApi() {
        return GroupedOpenApi.builder()
                .group("商品模块")
                .pathsToMatch("/api/goods/**")
                .build();
    }

    /**
     * 秒杀模块 API 分组
     */
    @Bean
    public GroupedOpenApi seckillApi() {
        return GroupedOpenApi.builder()
                .group("秒杀模块")
                .pathsToMatch("/api/seckill/**")
                .build();
    }

    /**
     * 订单模块 API 分组
     */
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("订单模块")
                .pathsToMatch("/api/order/**")
                .build();
    }

    /**
     * 管理后台 API 分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理后台")
                .pathsToMatch("/api/admin/**")
                .build();
    }

}
