package com.dm.cn.ragmilvus.controller;

import com.dm.cn.ragmilvus.config.MilvusUtils;
import com.dm.cn.ragmilvus.model.MilvusVo;
import io.milvus.client.MilvusServiceClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/rag/milvus")
public class BasicController {

    @Value("${milvus.databaseName}")
    private String milvusDatabaseName;

    @Value("${milvus.collectionName}")
    private String milvusCollectionName;

    private final VectorStore vectorStore;

    private final MilvusServiceClient milvusClient;

    private final EmbeddingModel embeddingModel;

    public BasicController(VectorStore vectorStore, MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.milvusClient = milvusClient;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 插入向量数据到 Milvus
     */
    @GetMapping("/add")
    public void addVectorData() {
        List<Document> documents = new ArrayList<>();
        // 定义10个不同的文本片段
        String[] chunks = {
                "Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!",
                "Spring Boot makes Java development easy and efficient for microservices.",
                "Milvus is an open-source vector database built for AI applications.",
                "Vector embedding transforms text into numerical vectors for semantic search.",
                "Spring AI integrates seamlessly with various AI models and vector databases.",
                "Document chunking improves the accuracy of semantic search in large texts.",
                "Milvus supports high-performance similarity search for massive vector datasets.",
                "Java Spring framework provides comprehensive support for enterprise applications.",
                "AI-powered search enhances user experience by understanding semantic meaning.",
                "Vector databases are essential for modern AI applications like RAG systems."
        };
        for (int i = 0; i < 10; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", UUID.randomUUID().toString());          // 唯一ID
            metadata.put("doc_id", "doc_" + (i + 1));                  // 文档ID（doc_1 到 doc_10）
            metadata.put("owner_id", 1001L + i);                       // 所有者ID（1001 到 1010）
            metadata.put("visibility", 1);                             // 可见性
            metadata.put("group_ids_csv", "group1,group2,group" + (i + 1)); // 分组（增加不同的分组）
            metadata.put("source", "manual_input");                    // 数据源
            metadata.put("chunk_index", i);                            // 分片索引
            // 创建Document并添加到列表
            Document document = new Document(chunks[i], metadata);
            documents.add(document);
        }

        MilvusUtils milvusUtils = new MilvusUtils();
        milvusUtils.createCollection(milvusClient, milvusDatabaseName, milvusCollectionName);
        milvusUtils.insert(milvusClient, milvusCollectionName, embeddingModel, documents);
    }

    /**
     * Milvus 向量数据更新
     */
    @GetMapping("/update")
    public void updateVectorData() {
        String chunk = "Spring AI Update Test";

        Map<String, Object> map = new HashMap<>();
        map.put("id", "03b852d2-2c11-41b4-b656-1f15889d1a9e");
        map.put("doc_id", "doc_1");
        map.put("owner_id", 1001L);
        map.put("visibility", 1);
        map.put("group_ids_csv", "group1,group2");
        map.put("source", "manual_input");
        map.put("chunk_index", 0);

        Document document = new Document(chunk, map);

        new MilvusUtils().update(milvusClient, milvusCollectionName, embeddingModel, List.of(document));
    }

    /**
     * Milvus 向量数据更新
     */
    @GetMapping("/delete")
    public void deleteVectorData(@RequestParam("id") String id) {
        new MilvusUtils().delete(milvusClient, milvusCollectionName, List.of(id));
    }

    /**
     * 相似度查询
     * @param query 查询向量
     * @param topK 返回前 K 条
     */
    @GetMapping("/search")
    public List<MilvusVo> similaritySearch(@RequestParam("query") String query, @RequestParam("topK") int topK, @RequestParam("filter") String filter) {
        List<MilvusVo> result = new MilvusUtils().search(milvusClient, milvusCollectionName, embeddingModel, query, topK, filter);
        return result;
    }
}
