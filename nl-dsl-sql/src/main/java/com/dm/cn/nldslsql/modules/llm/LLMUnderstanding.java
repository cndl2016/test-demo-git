package com.dm.cn.nldslsql.modules.llm;

import org.springframework.ai.chat.client.ChatClient;

import java.util.*;

/**
 * LLM语义理解模块
 * 协调LLM解析器、意图提取器、实体提取器和关系推理器
 * 将自然语言查询转换为结构化的语义结果
 */
public class LLMUnderstanding {
    private final Map<String, Object> schema;
    private final ChatClient chatClient;
    private final LLMParser parser;
    private final IntentExtractor intentExtractor;
    private final EntityExtractor entityExtractor;
    private final RelationReasoner relationReasoner;

    public LLMUnderstanding(ChatClient chatClient, Map<String, Object> schema) {
        this.schema = schema;
        this.chatClient = chatClient;
        this.parser = new LLMParser(chatClient, schema);
        this.intentExtractor = new IntentExtractor();
        this.entityExtractor = new EntityExtractor(schema);
        this.relationReasoner = new RelationReasoner(schema);
    }

    /**
     * 处理查询，进行语义理解
     * @param query 自然语言查询
     * @param preprocessed 预处理结果
     * @return 包含意图、表、字段、条件、关联等信息的语义结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(String query, Map<String, Object> preprocessed) {
        // 使用LLM解析查询
        Map<String, Object> parsed = parser.parse(query, preprocessed);
        String intent = intentExtractor.extract(query, preprocessed);

        // 提取涉及的表名（如果LLM解析未提供，则使用实体提取）
        List<String> tables = (List<String>) parsed.get("tables");
        if (tables == null || tables.isEmpty()) {
            tables = entityExtractor.extractTables(query, preprocessed);
        }

        // 提取涉及的字段（如果LLM解析未提供，则使用实体提取）
        List<String> fields = (List<String>) parsed.get("fields");
        if (fields == null || fields.isEmpty()) {
            fields = entityExtractor.extractFields(query, preprocessed);
        }

        // 推理表之间的关联关系
        List<Map<String, Object>> joins = relationReasoner.inferJoins(tables);
        Map<String, Object> relationships = relationReasoner.inferRelationships(tables);

        Map<String, Object> result = new HashMap<>();
        result.put("intent", intent);
        result.put("tables", tables);
        result.put("fields", fields);
        result.put("conditions", parsed.getOrDefault("conditions", new ArrayList<>()));
        result.put("aggregations", parsed.getOrDefault("aggregations", new ArrayList<>()));
        result.put("order_by", parsed.get("order_by"));
        result.put("group_by", parsed.getOrDefault("group_by", new ArrayList<>()));
        result.put("limit", parsed.get("limit"));
        result.put("joins", joins);
        result.put("relationships", relationships);
        result.put("raw_parsing", parsed);
        return result;
    }
}
