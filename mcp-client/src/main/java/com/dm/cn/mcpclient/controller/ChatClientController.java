package com.dm.cn.mcpclient.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/mcp/chat-client")
public class ChatClientController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己！";

    @Autowired
    ToolCallbackProvider tools;

    private final ChatClient chatClient;

    public ChatClientController(ChatModel chatModel) {
        // 构造时，可以设置 ChatClient 的参数
        // {@link org.springframework.ai.chat.client.ChatClient};
        this.chatClient = ChatClient.builder(chatModel)
                // 实现 Chat Memory 的 Advisor
                // 在使用 Chat Memory 时，需要指定对话 ID，以便 Spring AI 处理上下文。
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build()
                )
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }

    /**
     * 展示可用工具列表
     */
    @GetMapping("/showAvailableTools")
    public String showAvailableTools() {

        List<ToolDescription> toolDescriptions = chatClient
                .prompt("What tools are available? Please list them and avoid any additional comments. Only JSON format.")
                .toolCallbacks(tools)
                .call()
                .entity(new ParameterizedTypeReference<>() {});

        return toolDescriptions.stream().map(td -> td.toString()).collect(Collectors.joining("\n"));
    }

    private record ToolDescription(String toolName, String toolDescription) {
        @Override
        public final String toString() {
            return "Tool: " + toolName + " -> " + toolDescription;
        }
    }

    /**
     * ChatClient 简单调用
     */
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query) {
        return chatClient.prompt(query).toolCallbacks(tools).call().content();
    }

    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(query).toolCallbacks(tools).stream().content();
    }
}
