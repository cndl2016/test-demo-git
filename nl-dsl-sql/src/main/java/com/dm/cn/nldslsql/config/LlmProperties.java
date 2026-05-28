package com.dm.cn.nldslsql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM模型配置属性类
 * 用于配置大语言模型的提供商、模型名称、温度参数等
 */
@Data
@ConfigurationProperties(prefix = "nl2dslsql.llm")
public class LlmProperties {
    /** LLM提供商，默认为openai */
    private String provider = "openai";
    /** 模型名称，默认为gpt-4 */
    private String model = "gpt-4";
    /** 温度参数，控制输出随机性，默认为0.0（确定性输出） */
    private double temperature = 0.0;
    /** API密钥 */
    private String apiKey;
    /** API基础URL */
    private String baseUrl;
}
