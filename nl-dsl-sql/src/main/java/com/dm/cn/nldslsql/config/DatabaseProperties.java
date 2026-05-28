package com.dm.cn.nldslsql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 数据库配置属性类
 * 用于配置默认SQL方言和支持的数据库方言列表
 */
@Data
@ConfigurationProperties(prefix = "nl2dslsql.database")
public class DatabaseProperties {
    /** 默认SQL方言，默认为postgresql */
    private String defaultDialect = "postgresql";
    /** 支持的数据库方言列表 */
    private List<String> supportedDialects = List.of("postgresql", "mysql", "sqlite");
}
