package com.dm.cn.nldslsql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NL2DSL2SQL模块的主配置属性类
 * 通过application.yml中的nl2dslsql前缀进行配置绑定
 */
@Data
@ConfigurationProperties(prefix = "nl2dslsql")
public class Nl2Dsl2SqlProperties {
    /** LLM相关配置 */
    private LlmProperties llm = new LlmProperties();
    /** 数据库相关配置 */
    private DatabaseProperties database = new DatabaseProperties();
    /** 安全相关配置 */
    private SecurityProperties security = new SecurityProperties();
    /** 输出格式，默认为plain */
    private String outputFormat = "plain";
}
