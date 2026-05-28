package com.dm.cn.nldslsql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全配置属性类
 * 用于配置SQL注入检查、查询深度限制和允许的操作类型
 */
@Data
@ConfigurationProperties(prefix = "nl2dslsql.security")
public class SecurityProperties {
    /** 是否启用SQL注入检查，默认为true */
    private boolean enableSqlInjectionCheck = true;
    /** 最大查询深度，默认为10 */
    private int maxQueryDepth = 10;
    /** 允许的数据库操作类型，默认只允许SELECT */
    private List<String> allowedOperations = List.of("SELECT");
}
