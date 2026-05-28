package com.dm.cn.mcpclient.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class CustomMcpAsyncClientCustomizer implements McpAsyncClientCustomizer {
    @Override
    public void customize(String serverConfigurationName, McpClient.AsyncSpec spec) {

        // 自定义请求超时配置
        spec.requestTimeout(Duration.ofSeconds(30));

        // 设置此客户端可访问的根URI（用于限制请求范围）
        spec.roots();

        // 设置自定义采样处理器，用于处理消息创建请求
        spec.sampling((McpSchema.CreateMessageRequest messageRequest) -> {
            // 处理采样逻辑（例如生成消息结果）
            return null;
        });

        // 添加工具变更监听器，当可用工具列表发生变化时（如新增或移除工具）触发
        spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
            // 处理工具变更逻辑（例如更新本地工具缓存）
            System.out.println("工具Async变化:" + tools);
            return null;
        });

        // 添加资源变更监听器，当可用资源列表发生变化时（如新增或移除资源）触发
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            // 处理资源变更逻辑（例如更新资源元数据）
            System.out.println("元数据Async变化:" + resources);
            return null;
        });

        // 添加提示词变更监听器，当可用提示词列表发生变化时（如新增或移除提示词）触发
        spec.promptsChangeConsumer((List<McpSchema.Prompt> prompts) -> {
            // 处理提示词变更逻辑（例如重新加载提示词模板）
            System.out.println("提示词Async变化:" + prompts);
            return null;
        });

        // 添加日志消息监听器，当接收到服务端发送的日志消息时触发
        spec.loggingConsumer((McpSchema.LoggingMessageNotification log) -> {
            // 处理日志消息逻辑（例如记录到本地日志系统）
            System.out.println("日志Async变化:" + log);
            return null;
        });
    }
}