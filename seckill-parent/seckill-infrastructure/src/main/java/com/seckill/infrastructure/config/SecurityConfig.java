package com.seckill.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（开发环境）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // Knife4j 文档相关接口允许匿名访问
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/favicon.ico"
                        ).permitAll()
                        // 测试接口允许匿名访问
                        .requestMatchers("/api/test/**").permitAll()
                        // 健康检查接口允许匿名访问
                        .requestMatchers("/actuator/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 使用 HTTP Basic 认证（开发环境）
                .httpBasic(AbstractHttpConfigurer::disable)
                // 使用表单登录
                .formLogin(AbstractHttpConfigurer::disable);

        log.info("Security 配置完成 - Knife4j 文档和测试接口已开放匿名访问");
        return http.build();
    }
}
