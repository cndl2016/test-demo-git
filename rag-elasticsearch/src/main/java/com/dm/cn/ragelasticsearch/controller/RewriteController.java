package com.dm.cn.ragelasticsearch.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag/elasticsearch")
public class RewriteController {

	private final ChatClient chatClient;

	private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

	public RewriteController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {

		this.chatClient = chatClientBuilder.build();

		var documentRetriever = VectorStoreDocumentRetriever.builder()
			.vectorStore(vectorStore)
			.similarityThreshold(0.50)
			.build();

		// 重写查询转换器
		// RewriteQueryTransformer 使用大型语言模型重写用户查询，以便在查询目标系统（如矢量存储或 Web 搜索引擎）时提供更好的结果。
		// 当用户查询冗长、模棱两可或包含可能影响搜索结果质量的不相关信息时，此转换器非常有用。
		var queryTransformer = RewriteQueryTransformer.builder()
			.chatClientBuilder(chatClientBuilder.build().mutate())
			.targetSearchSystem("vector store")
			.build();

		this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(documentRetriever)
			.queryTransformers(queryTransformer)
			.build();
	}

	@PostMapping("/rewrite")
	public String rag(@RequestBody String prompt) {

		return chatClient.prompt().advisors(retrievalAugmentationAdvisor).user(prompt).call().content();
	}

}
