package com.dm.cn.ragelasticsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.dm.cn.ragelasticsearch.config.MyTextSplitter;
import com.dm.cn.ragelasticsearch.model.DocumentChunk;
import com.dm.cn.ragelasticsearch.model.SearchModel;
import com.dm.cn.ragelasticsearch.model.TextSplitterConfig;
import com.rometools.rome.feed.rss.Guid;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rag/vector")
public class VectorController {

    private static final Logger logger = LoggerFactory.getLogger(VectorController.class);

    private final VectorStore vectorStore;

    private final ElasticsearchClient elasticsearchClient;

    private final ElasticsearchVectorStoreProperties options;

    private static final String textField = "content";

    private static final String vectorField = "embedding";

    private static final int BATCH_SIZE = 10; // 向量数据库允许的最大批量大小:text-embedding-v4模型最大行数10

    public VectorController(
            VectorStore vectorStore,
            ElasticsearchClient elasticsearchClient,
            ElasticsearchVectorStoreProperties options) {

        this.vectorStore = vectorStore;
        this.elasticsearchClient = elasticsearchClient;
        this.options = options;
    }

    /**
     * 添加
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> initializeVectorDB(@RequestBody TextSplitterConfig config) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("文档读取");

            FileSystemResource resource = new FileSystemResource(config.getFilePath());
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
            List<Document> documents = tikaDocumentReader.read();
            documents.forEach(doc -> {
                doc.getMetadata().put("location", "波兰");
                doc.getMetadata().put("ref_doc_id", UUID.randomUUID().toString());
            });

            logger.info("创建和存储向量");

            createIndexIfNotExists();

            List<Document> splitDocuments = new ArrayList<>();
            if (config.getSplitterType().equalsIgnoreCase("default")) {
                // 默认token分割
                splitDocuments = new TokenTextSplitter().split(documents);
            } else if (config.getSplitterType().equalsIgnoreCase("custom")) {
                // 自定义分割
                splitDocuments = new MyTextSplitter(
                        config.getChunkSize(),
                        config.getKeepSeparator(),
                        config.getDelimiters(),
                        config.getIgnoreWhitespace(),
                        config.getOverlap()).split(documents);
            }
            logger.info("文档分割后共 {} 个片段", splitDocuments.size());
            splitDocuments.forEach(doc -> {
                logger.info(doc.getText());
            });

            // 分批添加到向量数据库
            int successCount = 0;
            for (int i = 0; i < splitDocuments.size(); i += BATCH_SIZE) {
                int toIndex = Math.min(i + BATCH_SIZE, splitDocuments.size());
                List<Document> batch = splitDocuments.subList(i, toIndex);

                try {
                    vectorStore.add(batch);
                    successCount += batch.size();
                    logger.info("成功添加批次: {} - {}", i, toIndex);
                } catch (Exception e) {
                    logger.error("添加批次失败: {} - {}", i, toIndex, e);
                }
            }

            response.put("success", true);
            response.put("message", "向量数据库初始化完成");
            response.put("分割文档", splitDocuments.size());
            response.put("成功添加", successCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("向量数据库初始化失败", e);
            response.put("success", false);
            response.put("message", "初始化失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 如果索引不存在则创建它
     */
    private void createIndexIfNotExists() {
        try {
            // 获取索引名称和向量维度
            String indexName = options.getIndexName();
            Integer dimsLength = options.getDimensions();

            // 检查索引名称是否为空
            if (StringUtils.isBlank(indexName)) {
                throw new IllegalArgumentException("必须提供Elasticsearch索引名称");
            }

            // 检查索引是否已存在
            boolean exists = elasticsearchClient.indices().exists(idx -> idx.index(indexName)).value();
            if (exists) {
                logger.debug("索引 {} 已存在。跳过创建。", indexName);
                return;
            }

            // 获取相似度算法名称
            String similarityAlgo = options.getSimilarity().name();

            // 设置索引配置：1个分片，1个副本
            IndexSettings indexSettings = IndexSettings
                    .of(settings -> settings.numberOfShards(String.valueOf(1)).numberOfReplicas(String.valueOf(1)));

            // 定义索引的属性映射
            Map<String, Property> properties = new HashMap<>();

            // 添加向量字段配置
            properties.put(vectorField, Property.of(property -> property.denseVector(
                    DenseVectorProperty.of(dense -> dense.index(true).dims(dimsLength)
                            .similarity(DenseVectorSimilarity.valueOf(similarityAlgo.toUpperCase()))))));

            // 添加文本字段配置
            properties.put(textField, Property.of(property -> property.text(TextProperty.of(t -> t))));

            // 定义元数据字段
            Map<String, Property> metadata = new HashMap<>();
            metadata.put("ref_doc_id", Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));

            // 将元数据作为对象属性添加
            properties.put("metadata", Property.of(property -> property.object(ObjectProperty.of(op -> op.properties(metadata)))));

            // 创建索引
            CreateIndexResponse indexResponse = elasticsearchClient.indices()
                    .create(createIndexBuilder -> createIndexBuilder.index(indexName)
                            .settings(indexSettings)
                            .mappings(TypeMapping.of(mappings -> mappings.properties(properties))));

            // 检查索引是否创建成功
            if (!indexResponse.acknowledged()) {
                throw new RuntimeException("创建索引失败");
            }

            logger.info("成功创建Elasticsearch索引 {}", indexName);
        } catch (IOException e) {
            logger.error("创建索引失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询
     */
    @PostMapping("/query")
    public List<DocumentChunk> query(@RequestBody SearchModel model) throws IOException {
        String indexName = options.getIndexName();
        String source = model.getSource();
        String location = model.getLocation();
        String refDocId = model.getRefDocId();
        Integer pageIndex = model.getPageIndex();
        Integer pageSize = model.getPageSize();

        // 构建SearchRequest
        SearchRequest request = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        // 使用bool查询组合多个条件
                        .bool(b -> {
                            if (source != null && !source.isEmpty()) {
                                b.must(m -> m
                                        .term(t -> t
                                                .field("metadata.source.keyword")
                                                .value(source)
                                        )
                                );
                            }
                            if (location != null && !location.isEmpty()) {
                                b.must(m -> m
                                        .match(t -> t
                                                .field("metadata.location")
                                                .query(location)
                                        )
                                );
                            }
                            if (refDocId != null && !refDocId.isEmpty()) {
                                b.must(m -> m
                                        .term(t -> t
                                                .field("metadata.ref_doc_id")
                                                .value(refDocId)
                                        )
                                );
                            }
                            return b;
                        })
                )
                .from(pageIndex)
                .size(pageSize)
        );

        // 执行查询
        SearchResponse<DocumentChunk> response = elasticsearchClient.search(request, DocumentChunk.class);
        // 提取查询结果
        List<DocumentChunk> result = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteIndex(@RequestBody String indexName) throws IOException {
        Map<String, Object> response = new HashMap<>();
        try {
            // 先判断索引是否存在
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                // 存在则删除
                DeleteIndexRequest request = DeleteIndexRequest.of(d -> d.index(indexName));
                // 执行删除操作
                boolean deleted = elasticsearchClient.indices().delete(request).acknowledged();
                if (deleted) {
                    response.put("success", true);
                    response.put("message", "索引删除成功");
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "索引删除失败");
                    return ResponseEntity.ok(response);
                }
            } else {
                response.put("success", true);
                response.put("message", "索引不存在，无需删除");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
