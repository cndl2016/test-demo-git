package com.dm.cn.nldslsql.modules.dslgen;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.Join;

import java.util.*;

/**
 * DSL构建器
 * 将LLM或规则引擎解析出的语义结果（Map结构）转换为类型安全的DSL对象
 * 包含对条件、关联、排序、聚合等元素的防御性解析，兼容不同LLM输出格式
 */
public class DSLBuilder {
    private final Map<String, Object> schema;

    public DSLBuilder(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * 根据语义解析结果构建DSL对象
     * @param semanticResult 语义解析结果，包含intent、tables、fields、conditions等
     * @return 构建好的DSL对象
     */
    @SuppressWarnings("unchecked")
    public DSL build(Map<String, Object> semanticResult) {
        // 提取操作类型，默认为SELECT
        String operation = (String) semanticResult.getOrDefault("intent", "SELECT");

        // 提取涉及的数据表，若未指定则使用schema中的第一张表作为默认表
        List<String> tables = (List<String>) semanticResult.get("tables");
        if (tables == null || tables.isEmpty()) {
            Map<String, Object> tablesMap = (Map<String, Object>) schema.get("tables");
            if (tablesMap != null && !tablesMap.isEmpty()) {
                tables = new ArrayList<>();
                tables.add(tablesMap.keySet().iterator().next());
            } else {
                tables = new ArrayList<>();
            }
        }

        // 提取查询字段，若未指定则默认查询所有字段
        List<String> fields = (List<String>) semanticResult.get("fields");
        if (fields == null || fields.isEmpty()) {
            fields = new ArrayList<>();
            fields.add("*");
        }

        // 解析条件列表：防御性处理，支持对象格式和字符串格式
        List<Condition> conditions = new ArrayList<>();
        List<?> condList = (List<?>) semanticResult.get("conditions");
        if (condList != null) {
            for (Object item : condList) {
                // 仅当条件项为Map格式时才解析，避免ClassCastException
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cond = (Map<String, Object>) item;
                    conditions.add(new Condition(
                            String.valueOf(cond.getOrDefault("field", "")),
                            String.valueOf(cond.getOrDefault("operator", "=")),
                            cond.getOrDefault("value", ""),
                            String.valueOf(cond.getOrDefault("logical", "AND"))
                    ));
                }
            }
        }

        // 解析表关联（JOIN）信息：同样使用防御性类型检查
        List<Join> joins = new ArrayList<>();
        List<?> joinList = (List<?>) semanticResult.get("joins");
        if (joinList != null) {
            for (Object item : joinList) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> join = (Map<String, Object>) item;
                    joins.add(new Join(
                            String.valueOf(join.getOrDefault("type", "INNER JOIN")),
                            String.valueOf(join.getOrDefault("from_table", "")),
                            String.valueOf(join.getOrDefault("from_field", "")),
                            String.valueOf(join.getOrDefault("to_table", "")),
                            String.valueOf(join.getOrDefault("to_field", ""))
                    ));
                }
            }
        }

        // 提取分组字段
        List<String> groupBy = (List<String>) semanticResult.getOrDefault("group_by", new ArrayList<>());

        // 解析排序信息：支持字符串格式和Map格式
        List<Map<String, String>> orderBy = new ArrayList<>();
        Object orderByRaw = semanticResult.get("order_by");
        if (orderByRaw instanceof String) {
            // 字符串格式："field ASC" 或 "field"
            String[] parts = ((String) orderByRaw).split("\\s+");
            if (parts.length >= 2) {
                Map<String, String> order = new HashMap<>();
                order.put("field", parts[0]);
                order.put("direction", parts[1]);
                orderBy.add(order);
            } else if (parts.length == 1) {
                Map<String, String> order = new HashMap<>();
                order.put("field", parts[0]);
                order.put("direction", "ASC");
                orderBy.add(order);
            }
        } else if (orderByRaw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> orderMap = (Map<String, String>) orderByRaw;
            orderBy.add(orderMap);
        }

        // 解析limit限制数量
        Integer limit = null;
        Object limitObj = semanticResult.get("limit");
        if (limitObj != null) {
            limit = Integer.parseInt(String.valueOf(limitObj));
        }

        // 解析聚合函数：支持字符串列表和对象列表两种格式
        List<String> aggregations = new ArrayList<>();
        Object aggRaw = semanticResult.get("aggregations");
        if (aggRaw instanceof List) {
            for (Object agg : (List<?>) aggRaw) {
                if (agg instanceof String) {
                    aggregations.add((String) agg);
                } else if (agg instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> aggMap = (Map<String, Object>) agg;
                    String func = (String) aggMap.getOrDefault("function", aggMap.get("func"));
                    if (func != null) {
                        aggregations.add(func);
                    }
                }
            }
        }

        // 组装DSL对象
        DSL dsl = new DSL();
        dsl.setOperation(operation);
        dsl.setTables(tables);
        dsl.setFields(fields);
        dsl.setConditions(conditions);
        dsl.setJoins(joins);
        dsl.setGroupBy(groupBy);
        dsl.setOrderBy(orderBy);
        dsl.setLimit(limit);
        dsl.setAggregations(aggregations);
        return dsl;
    }
}
