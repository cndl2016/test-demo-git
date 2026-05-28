package com.dm.cn.ragmilvus.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private Integer milvusPort;

    @Value("${milvus.username:root}")
    private String milvusUsername;

    @Value("${milvus.password:milvus}")
    private String milvusPassword;

    @Value("${milvus.databaseName:default}")
    private String milvusDatabaseName;

    @Value("${milvus.collectionName:default}")
    private String milvusCollectionName;

    @Bean
    public VectorStore vectorStore(MilvusServiceClient milvusClient, EmbeddingModel embeddingModel) {
        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .databaseName(milvusDatabaseName)
                .collectionName(milvusCollectionName)
                .iDFieldName("id")
                .embeddingFieldName("embedding")
                .metadataFieldName("metadata")
                .contentFieldName("chunk")
                .indexType(IndexType.HNSW)
                .metricType(MetricType.COSINE)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .initializeSchema(false) // 禁用自动创建（已手动创建）
                .build();
    }

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withAuthorization(milvusUsername, milvusPassword)
                .build();
        return new MilvusServiceClient(connectParam);
    }
}