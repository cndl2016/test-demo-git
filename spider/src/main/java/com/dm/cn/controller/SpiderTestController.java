package com.dm.cn.controller;

import com.dm.cn.model.SqlQuery;
import com.dm.cn.service.SpiderTestService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Spider测试集测试控制器
 */
@RestController
@RequestMapping("/api/spider")
public class SpiderTestController {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    @Autowired
    private SpiderTestService spiderTestService;

    public SpiderTestController(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
//                        OllamaChatOptions.builder()
//                                .disableThinking()
                        OpenAiChatOptions.builder()
                                .build()
                )
                .build();
    }

    @PostMapping("/getSqlResultByClient")
    public List<SqlQuery> getSqlResultByClient(@RequestBody String param) {
        return spiderTestService.getSqlResultByClient(chatClient);
    }

    @PostMapping("/getSqlResultByModel")
    public List<SqlQuery> getSqlResultByModel(@RequestBody String param) {
        return spiderTestService.getSqlResultByModel(chatModel);
    }
}