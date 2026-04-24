package com.seckill.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 电商秒杀系统 - 应用启动入口
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.seckill")
public class SeckillApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(SeckillApplication.class, args);
        Environment env = context.getEnvironment();

        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info("\n----------------------------------------------------------\n" +
                "\t电商秒杀系统启动成功!\n" +
                "----------------------------------------------------------\n" +
                "\t本地访问: http://localhost:{}{}\n" +
                "\t外部访问: http://{}:{}{}\n" +
                "\tAPI文档: http://{}:{}{}/doc.html\n" +
                "----------------------------------------------------------",
                port, contextPath,
                ip, port, contextPath,
                ip, port, contextPath);
    }
}
