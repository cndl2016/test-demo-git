package com.dm.cn.ragelasticsearch.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/rag/elasticsearch")
public class CompressionController {

	private final ChatClient chatClient;

	private final MessageChatMemoryAdvisor chatMemoryAdvisor;

	private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

	public CompressionController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                                 VectorStore vectorStore) {

		this.chatClient = chatClientBuilder.build();

		this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

		var documentRetriever = VectorStoreDocumentRetriever.builder()
			.vectorStore(vectorStore)
			.similarityThreshold(0.50)
			.build();

		// 压缩查询转换器
		// CompressionQueryTransformer 使用大型语言模型将对话历史记录和后续查询压缩为捕获对话本质的独立查询。
		// 当对话历史记录很长并且后续查询与对话上下文相关时，此转换器非常有用
		var queryTransformer = CompressionQueryTransformer.builder()
			.chatClientBuilder(chatClientBuilder.build().mutate())
			.build();

		this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(documentRetriever)
			.queryTransformers(queryTransformer)
			.build();
	}

	@PostMapping("/compression/{chatId}")
	public String rag(@RequestBody String prompt, @PathVariable("chatId") String conversationId) {

		return chatClient.prompt()
			.advisors(chatMemoryAdvisor, retrievalAugmentationAdvisor)
			.advisors(advisors -> advisors.param(CONVERSATION_ID,
					conversationId))
			.user(prompt)
			.call()
			.content();
	}

}
