package com.distributed.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 分布式基础工程启动类
 *
 * <p>技术栈：Spring Boot 3.x + MongoDB + Redis + Apache CXF (SOAP)
 *
 * <p>本地开发：mvn spring-boot:run -Pdev
 * 生产部署：mvn package -Pprod && java -jar target/distributed-base-*.jar
 */
@SpringBootApplication
public class DistributedBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedBaseApplication.class, args);
    }
}
