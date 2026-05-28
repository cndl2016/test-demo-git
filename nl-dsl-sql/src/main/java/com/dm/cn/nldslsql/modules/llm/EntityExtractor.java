package com.dm.cn.nldslsql.modules.llm;

import java.util.*;

/**
 * 实体提取器
 * 从自然语言查询中提取涉及的表名和字段名
 * 作为LLM解析的补充，当LLM未识别到表/字段时使用
 */
public class EntityExtractor {
    private final Map<String, Object> schema;

    public EntityExtractor(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * 从查询中提取涉及的表名
     * @param query 查询字符串
     * @param preprocessed 预处理结果
     * @return 表名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> extractTables(String query, Map<String, Object> preprocessed) {
        // 优先使用预处理阶段已识别的表
        List<String> tables = (List<String>) preprocessed.get("tables");
        if (tables != null && !tables.isEmpty()) {
            return tables;
        }

        tables = new ArrayList<>();
        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        if (tablesDict == null) return tables;

        // 通过查询字符串匹配表名
        List<String> availableTables = new ArrayList<>(tablesDict.keySet());
        String queryLower = query.toLowerCase();
        for (String table : availableTables) {
            if (queryLower.contains(table.toLowerCase())) {
                tables.add(table);
            }
        }
        return tables;
    }

    /**
     * 从查询中提取涉及的字段名
     * @param query 查询字符串
     * @param preprocessed 预处理结果
     * @return 字段名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> extractFields(String query, Map<String, Object> preprocessed) {
        // 优先使用预处理阶段已识别的字段
        List<String> fields = (List<String>) preprocessed.get("fields");
        if (fields != null && !fields.isEmpty()) {
            return fields;
        }

        fields = new ArrayList<>();
        List<String> allFields = new ArrayList<>();
        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        if (tablesDict == null) return fields;

        // 收集所有表的字段名
        for (Map.Entry<String, Object> entry : tablesDict.entrySet()) {
            Map<String, Object> tableInfo = (Map<String, Object>) entry.getValue();
            List<String> fieldList = (List<String>) tableInfo.get("columns");
            if (fieldList != null) {
                allFields.addAll(fieldList);
            }
        }

        // 通过查询字符串匹配字段名
        String queryLower = query.toLowerCase();
        for (String field : allFields) {
            if (queryLower.contains(field.toLowerCase())) {
                fields.add(field);
            }
        }
        return fields;
    }
}
