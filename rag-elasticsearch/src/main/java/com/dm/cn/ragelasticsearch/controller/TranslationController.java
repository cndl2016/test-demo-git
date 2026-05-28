package com.dm.cn.ragelasticsearch.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag/elasticsearch")
public class TranslationController {

	private final ChatClient chatClient;

	private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

	public TranslationController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
		this.chatClient = chatClientBuilder.build();

		var documentRetriever = VectorStoreDocumentRetriever.builder()
			.vectorStore(vectorStore)
			.similarityThreshold(0.50)
			.build();

		// 翻译查询转换器
		// TranslationQueryTransformer 使用大型语言模型将查询转换为用于生成文档嵌入的嵌入模型支持的目标语言。如果查询已使用目标语言，则返回该查询时不会更改。如果查询的语言未知，则也会原封不动地返回它。
		// 当嵌入模型使用特定语言进行训练并且用户查询使用其他语言时，此转换器非常有用。
		var queryTransformer = TranslationQueryTransformer.builder()
			.chatClientBuilder(chatClientBuilder.build().mutate())
			.targetLanguage("english")
			.build();

		this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(documentRetriever)
			.queryTransformers(queryTransformer)
			.build();
	}

	@PostMapping("/translation")
	public String rag(@RequestBody String prompt) {

		return chatClient.prompt().advisors(retrievalAugmentationAdvisor).user(prompt).call().content();
	}

}
