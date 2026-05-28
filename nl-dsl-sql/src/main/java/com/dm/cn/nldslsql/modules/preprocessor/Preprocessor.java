package com.dm.cn.nldslsql.modules.preprocessor;

import java.util.*;

/**
 * 查询预处理器
 * 负责对用户输入的自然语言查询进行标准化、意图分类和实体识别
 * 是整个NL2SQL流水线的第一阶段
 */
public class Preprocessor {
    private final Map<String, Object> schema;
    private final QueryNormalizer normalizer;
    private final EntityRecognizer entityRecognizer;
    private final IntentClassifier intentClassifier;
    private final ContextManager contextManager;

    public Preprocessor(Map<String, Object> schema) {
        this.schema = schema;
        this.normalizer = new QueryNormalizer();
        this.entityRecognizer = new EntityRecognizer();
        this.intentClassifier = new IntentClassifier();
        this.contextManager = new ContextManager();
    }

    /**
     * 处理自然语言查询，返回预处理结果
     * @param query 原始用户查询
     * @return 包含标准化查询、意图、识别出的表和字段等信息的Map
     */
    public Map<String, Object> process(String query) {
        // 查询标准化：去除特殊字符、合并多余空格
        String normalizedQuery = normalizer.normalize(query);
        // 意图分类：判断是SELECT/INSERT/UPDATE/DELETE
        String intent = intentClassifier.classify(normalizedQuery);
        // 实体识别：从查询中提取涉及的表名和字段名
        List<String> tables = entityRecognizer.recognizeTables(normalizedQuery, schema);
        List<String> fields = entityRecognizer.recognizeFields(normalizedQuery, schema);

        // 更新上下文管理器
        contextManager.addContext(normalizedQuery, intent, tables);

        Map<String, Object> result = new HashMap<>();
        result.put("original_query", query);
        result.put("normalized_query", normalizedQuery);
        result.put("intent", intent);
        result.put("tables", tables);
        result.put("fields", fields);
        result.put("context", contextManager.getLastContext());
        return result;
    }
}
