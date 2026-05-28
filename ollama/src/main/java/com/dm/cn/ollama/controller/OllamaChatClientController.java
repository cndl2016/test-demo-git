package com.dm.cn.ollama.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ollama/chat-client")
public class OllamaChatClientController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己！";

    private final ChatClient chatClient;

    @Autowired
    JdbcChatMemoryRepository chatMemoryRepository;

    public OllamaChatClientController(ChatModel chatModel) {
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
                        OllamaChatOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }

    // 也可以使用如下的方式注入 ChatClient
    // public OpenAIChatClientController(ChatClient.Builder chatClientBuilder) {
    //
    //  	this.chatClient = chatClientBuilder.build();
    // }

    /**
     * ChatClient 简单调用
     */
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query) {
        return chatClient.prompt(query).call().content();
    }

    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query, HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(query).stream().content();
    }

    /**
     * ChatClient 流式响应
     */
    @GetMapping(value = "/stream/response", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }

    /**
     * ChatClient 存储到 mysql
     */
    @GetMapping(value = "/mysql", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> mysql(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query,
                         @RequestParam("chatId") String chatId,
                         HttpServletResponse response) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();

        return chatClient.prompt()
                .user(query)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }
}
