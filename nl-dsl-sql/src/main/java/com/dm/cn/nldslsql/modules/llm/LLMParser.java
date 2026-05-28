package com.dm.cn.nldslsql.modules.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM解析器
 * 使用大语言模型将自然语言查询解析为结构化的语义结果（Map格式）
 * 同时提供规则引擎回退机制，在LLM不可用时通过正则表达式提取查询要素
 */
@Slf4j
public class LLMParser {
    private final ChatClient chatClient;
    private final Map<String, Object> schema;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMParser(ChatClient chatClient, Map<String, Object> schema) {
        this.chatClient = chatClient;
        this.schema = schema;
    }

    /**
     * 根据相关表名过滤Schema，减少LLM Prompt的噪声和Token消耗
     * @param schema 完整Schema
     * @param relevantTables 预处理阶段识别出的相关表
     * @return 过滤后的Schema（仅包含相关表和关联关系）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> filterSchema(Map<String, Object> schema, List<String> relevantTables) {
        if (relevantTables == null || relevantTables.isEmpty()) {
            return schema;
        }
        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        List<Map<String, String>> relations = (List<Map<String, String>>) schema.get("relations");

        // 保留相关表定义
        Map<String, Object> filteredTables = new HashMap<>();
        if (tablesDict != null) {
            for (String tableName : relevantTables) {
                if (tablesDict.containsKey(tableName)) {
                    filteredTables.put(tableName, tablesDict.get(tableName));
                }
            }
        }

        // 保留与相关表有关联的关系定义
        Set<String> tableSet = new HashSet<>(relevantTables);
        List<Map<String, String>> filteredRelations = new ArrayList<>();
        if (relations != null) {
            for (Map<String, String> rel : relations) {
                if (tableSet.contains(rel.get("from_table")) || tableSet.contains(rel.get("to_table"))) {
                    filteredRelations.add(rel);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tables", filteredTables);
        result.put("relations", filteredRelations);
        return result;
    }

    /**
     * 解析自然语言查询为语义结果Map
     * 优先使用LLM，若chatClient未配置则回退到规则解析
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String query, Map<String, Object> preprocessed) {
        if (chatClient != null) {
            return parseWithLlm(query, preprocessed);
        } else {
            return parseRuleBased(query, preprocessed);
        }
    }

    /**
     * 调用LLM进行语义解析
     */
    private Map<String, Object> parseWithLlm(String query, Map<String, Object> preprocessed) {
        // 基于预处理结果中的相关表过滤Schema，减少Prompt体积
        List<String> relevantTables = (List<String>) preprocessed.get("tables");
        Map<String, Object> filteredSchema = filterSchema(schema, relevantTables);
        String schemaStr;
        String relationsStr;
        try {
            schemaStr = objectMapper.writeValueAsString(filteredSchema);
            relationsStr = objectMapper.writeValueAsString(filteredSchema.getOrDefault("relations", new ArrayList<>()));
        } catch (JsonProcessingException e) {
            return parseRuleBased(query, preprocessed);
        }

        // 构建LLM Prompt，明确指定输出JSON格式和各字段要求
        String promptText = String.format(
                "你是一个SQL查询解析器。根据用户的自然语言查询和数据库schema，解析出查询意图。\n\n数据库schema:\n%s\n\n表之间的关联关系:\n%s\n\n用户查询: %s\n\n请返回JSON格式的解析结果，包含以下字段:\n- intent: 查询意图 (只支持SELECT)\n- tables: 涉及的表名列表\n- fields: 需要查询/操作的字段列表\n- conditions: 查询条件列表，每个条件是一个对象，格式为 {\"field\": \"字段名\", \"operator\": \"操作符\", \"value\": \"值\"}\n- aggregations: 聚合函数列表\n- order_by: 排序字段，格式为 {\"field\": \"字段名\", \"direction\": \"ASC或DESC\"}\n- group_by: 分组字段列表\n- limit: 限制数量(默认300，最多300条)\n- joins: 关联查询信息（多表关联时需要，包含from_table, from_field, to_table, to_field）\n\n只返回JSON，不要其他内容。",
                schemaStr, relationsStr, query
        );

        try {
            ChatResponse response = chatClient.prompt(new Prompt(promptText)).call().chatResponse();
            String content = response.getResult().getOutput().getText();
            Map<String, Object> result = objectMapper.readValue(content, new TypeReference<>() {});

            // 强制设置意图为SELECT，确保安全性
            result.put("intent", "SELECT");

            // 限制最大返回条数为300
            Object limitObj = result.get("limit");
            if (limitObj == null) {
                result.put("limit", 300);
            } else {
                int limit = Integer.parseInt(String.valueOf(limitObj));
                result.put("limit", Math.min(limit, 300));
            }

            return result;
        } catch (Exception e) {
            log.warn("LLM parse failed, fallback to rule based: {}", e.getMessage());
            return parseRuleBased(query, preprocessed);
        }
    }

    /**
     * 基于规则的解析回退方案
     * 当LLM不可用时，通过正则表达式提取查询中的表、字段、条件、聚合等要素
     */
    private Map<String, Object> parseRuleBased(String query, Map<String, Object> preprocessed) {
        Map<String, Object> result = new HashMap<>();
        result.put("intent", preprocessed.getOrDefault("intent", "SELECT"));
        result.put("tables", preprocessed.getOrDefault("tables", new ArrayList<>()));
        result.put("fields", preprocessed.getOrDefault("fields", new ArrayList<>()));
        result.put("conditions", extractConditions(query));
        result.put("aggregations", extractAggregations(query));
        result.put("order_by", extractOrderBy(query));
        result.put("group_by", extractGroupBy(query));
        result.put("limit", extractLimit(query));
        return result;
    }

    /**
     * 从查询中提取条件：支持中文和英文操作符
     */
    private List<Map<String, Object>> extractConditions(String query) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, String> operators = new HashMap<>();
        operators.put("大于", ">");
        operators.put("小于", "<");
        operators.put("等于", "=");
        operators.put("不等于", "!=");
        operators.put("大于等于", ">=");
        operators.put("小于等于", "<=");
        operators.put("包含", "LIKE");
        operators.put("gt", ">");
        operators.put("lt", "<");
        operators.put("eq", "=");
        operators.put("ne", "!=");

        for (Map.Entry<String, String> entry : operators.entrySet()) {
            if (query.contains(entry.getKey())) {
                String[] parts = query.split(Pattern.quote(entry.getKey()));
                if (parts.length == 2) {
                    String fieldPart = parts[0].trim();
                    String valuePart = parts[1].trim();
                    // 简单过滤：避免将DSL/JSON内容误识别为条件
                    if (!fieldPart.isEmpty() && !valuePart.isEmpty() && !fieldPart.startsWith("DSL")) {
                        Map<String, Object> cond = new HashMap<>();
                        cond.put("field", fieldPart);
                        cond.put("operator", entry.getValue());
                        cond.put("value", valuePart);
                        conditions.add(cond);
                    }
                }
            }
        }
        return conditions;
    }

    /**
     * 从查询中提取聚合函数关键词
     */
    private List<String> extractAggregations(String query) {
        List<String> aggs = new ArrayList<>();
        Map<String, String> aggKeywords = new HashMap<>();
        aggKeywords.put("统计", "COUNT");
        aggKeywords.put("数量", "COUNT");
        aggKeywords.put("count", "COUNT");
        aggKeywords.put("求和", "SUM");
        aggKeywords.put("总和", "SUM");
        aggKeywords.put("sum", "SUM");
        aggKeywords.put("平均", "AVG");
        aggKeywords.put("平均值", "AVG");
        aggKeywords.put("avg", "AVG");
        aggKeywords.put("最大", "MAX");
        aggKeywords.put("最大值", "MAX");
        aggKeywords.put("max", "MAX");
        aggKeywords.put("最小", "MIN");
        aggKeywords.put("最小值", "MIN");
        aggKeywords.put("min", "MIN");
        for (Map.Entry<String, String> entry : aggKeywords.entrySet()) {
            if (query.contains(entry.getKey())) {
                aggs.add(entry.getValue());
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(aggs));
    }

    /**
     * 从查询中提取排序信息
     */
    private Map<String, String> extractOrderBy(String query) {
        String[] patterns = {"按(.+?)排序", "按照(.+?)排序", "按(.+?)升序", "按(.+?)降序", "order by (\\w+)"};
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(query);
            if (matcher.find()) {
                String field = matcher.group(1);
                String direction = matcher.group(0).contains("降序") ? "DESC" : "ASC";
                Map<String, String> result = new HashMap<>();
                result.put("field", field);
                result.put("direction", direction);
                return result;
            }
        }
        return null;
    }

    /**
     * 从查询中提取分组信息
     */
    private List<String> extractGroupBy(String query) {
        List<String> groups = new ArrayList<>();
        String[] patterns = {"按(.+?)分组", "按照(.+?)分组", "group by (\\w+)"};
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(query);
            if (matcher.find()) {
                groups.add(matcher.group(1));
            }
        }
        return groups;
    }

    /**
     * 从查询中提取limit限制数量
     */
    private Integer extractLimit(String query) {
        String[] patterns = {"前(\\d+)条", "前(\\d+)个", "limit (\\d+)", "只取(\\d+)"};
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(query);
            if (matcher.find()) {
                int val = Integer.parseInt(matcher.group(1));
                return Math.min(val, 300);
            }
        }
        return 300;
    }
}
