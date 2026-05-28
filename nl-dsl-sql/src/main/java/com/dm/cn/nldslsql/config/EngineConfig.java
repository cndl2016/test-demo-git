package com.dm.cn.nldslsql.config;

import com.dm.cn.nldslsql.core.DatabaseSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 引擎核心配置类
 * 负责配置ChatClient和数据库Schema等核心组件
 */
@Configuration
public class EngineConfig {

    /**
     * 创建ChatClient实例，用于与LLM模型交互
     * @param chatModel Spring AI的聊天模型
     * @return ChatClient实例，如果chatModel为null则返回null
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        if (chatModel == null) {
            return null;
        }
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 创建示例数据库Schema，用于测试和演示
     * @return 包含示例表结构和关系的DatabaseSchema
     */
    @Bean
    public DatabaseSchema databaseSchema() {
        return DatabaseSchema.createSampleSchema();
    }
}
