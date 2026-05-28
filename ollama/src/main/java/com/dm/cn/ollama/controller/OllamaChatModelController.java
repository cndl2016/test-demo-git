package com.dm.cn.ollama.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ollama/chat-model")
public class OllamaChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";

    private final ChatModel chatModel;

    public OllamaChatModelController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 最简单的使用方式，没有任何 LLMs 参数注入。
     *
     * @return String types.
     */
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query) {
        return chatModel.call(new Prompt(query)).getResult().getOutput().getText();
    }

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     *
     * @return Flux<String> types.
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query, HttpServletResponse response) {

        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");

        Flux<ChatResponse> chatResponseFlux = chatModel.stream(new Prompt(query));
        return chatResponseFlux.map(resp -> resp.getResult().getOutput().getText());
    }

    /**
     * 使用编程方式自定义 LLMs ChatOptions 参数
     * 优先级高于在 application.yml 中配置的 LLMs 参数！
     */
    @GetMapping("/custom/chat")
    public String customChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query) {

        OllamaChatOptions customOptions = OllamaChatOptions.builder()
                .topP(0.7)
                .temperature(0.8)
                .build();

        return chatModel.call(new Prompt(query, customOptions)).getResult().getOutput().getText();
    }
}
