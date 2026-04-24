package com.seckill.application;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 秒杀系统应用启动类
 *
 * @author seckill
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.seckill"})
@MapperScan(basePackages = {"com.seckill.**.mapper"})
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
        System.out.println("==============================================");
        System.out.println("       电商秒杀系统启动成功！                  ");
        System.out.println("==============================================");
        System.out.println("API 文档地址: http://localhost:8080/doc.html");
        System.out.println("==============================================");
    }

}
