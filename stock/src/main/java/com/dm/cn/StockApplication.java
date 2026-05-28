package com.dm.cn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Stock模块启动类
 */
@SpringBootApplication
public class StockApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(StockApplication.class, args);
    }
}
