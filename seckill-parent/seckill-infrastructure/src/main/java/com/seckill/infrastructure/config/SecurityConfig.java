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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 使用 HTTP Basic 认证（开发环境）
                .httpBasic(AbstractHttpConfigurer::disable)
                // 使用表单登录
                .formLogin(AbstractHttpConfigurer::disable);

        log.info("Security 配置完成 - 请求鉴权统一交由 MVC 拦截器处理");
        return http.build();
    }
}
