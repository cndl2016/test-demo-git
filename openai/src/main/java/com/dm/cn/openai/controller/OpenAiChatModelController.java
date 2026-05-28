package com.dm.cn.openai.controller;

import com.dm.cn.openai.component.DateTimeTools;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/openai/chat-model")
public class OpenAiChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";
    private static final String JSON_OUTPUT_PROMPT = "JSON：how can I solve 8x + 7 = -23";

    private final ChatModel chatModel;

    public OpenAiChatModelController(ChatModel chatModel) {
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

        OpenAiChatOptions customOptions = OpenAiChatOptions.builder()
                .topP(0.7)
                .model("qwen-plus")
                .maxTokens(1000)
                .temperature(0.8)
                .build();

        return chatModel.call(new Prompt(query, customOptions)).getResult().getOutput().getText();
    }

    /**
     * JSON mode：通过设置 response_format 参数为 JSON 类型，使大模型返回标准的 JSON 格式数据。
     * <p>
     * OpenAI 的 JSON 响应格式要求
     * 当你设置response_format="json_object"时，OpenAI 要求：
     * 消息内容中必须提到 "json"、"JSON" 或类似词汇
     * 明确要求模型以 JSON 格式返回内容
     * 这是因为 JSON 格式是一种特殊响应模式，需要模型明确知道要生成 JSON 结构。
     *
     * @return JSON String.
     */
    @GetMapping("/custom/chat/json-mode")
    public String jsonChat(@RequestParam(value = "query", defaultValue = JSON_OUTPUT_PROMPT) String query) {

        String jsonSchema = """
                {
                    "type": "object",
                    "properties": {
                        "steps": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "explanation": { "type": "string" },
                                    "output": { "type": "string" }
                                },
                                "required": ["explanation", "output"],
                                "additionalProperties": false
                            }
                        },
                        "final_answer": { "type": "string" }
                    },
                    "required": ["steps", "final_answer"],
                    "additionalProperties": false
                }
                """;

        OpenAiChatOptions customOptions = OpenAiChatOptions.builder()
                .topP(0.7)
                .model("qwen-plus")
                .temperature(0.4)
                .maxTokens(4096)
                .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema))
                .build();

        return chatModel.call(new Prompt(query, customOptions)).getResult().getOutput().getText();
    }

    /**
     * ChatModel 工具 时间
     *
     * @return String types.
     */
    @GetMapping("/tool/datetime")
    public String datetime(@RequestParam(value = "query", defaultValue = "今天是几月几日？") String query) {
        ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(dateTimeTools)
                .build();
        return chatModel.call(new Prompt(query, chatOptions)).getResult().getOutput().getText();
    }

    /**
     * ChatModel 工具 闹钟
     *
     * @return String types.
     */
    @GetMapping("/tool/alarm")
    public String alarm(@RequestParam(value = "query", defaultValue = "请帮我设置一个1分钟后的闹钟") String query) {
        ToolCallback[] dateTimeTools = ToolCallbacks.from(new DateTimeTools());
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(dateTimeTools)
                .build();
        return chatModel.call(new Prompt(query, chatOptions)).getResult().getOutput().getText();
    }

}
