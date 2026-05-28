package com.dm.cn;

import com.dm.cn.nldslsql.config.Nl2Dsl2SqlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

/**
 * NL2DSL2SQL 应用程序入口类
 * 提供自然语言到DSL再到SQL的转换服务
 */
@SpringBootApplication
@EnableConfigurationProperties(Nl2Dsl2SqlProperties.class)
public class NL2DSL2SQLApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(NL2DSL2SQLApplication.class, args);
    }
}
