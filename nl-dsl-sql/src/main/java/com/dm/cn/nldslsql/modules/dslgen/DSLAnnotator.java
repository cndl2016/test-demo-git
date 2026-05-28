package com.dm.cn.nldslsql.modules.dslgen;

import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.Join;

import java.util.*;

/**
 * DSL注释器
 * 为DSL添加注解信息，包括原始查询、意图、置信度和推理说明
 */
public class DSLAnnotator {
    /**
     * 为DSL添加注释信息
     * @param dsl DSL对象
     * @param query 原始自然语言查询
     * @param semanticResult 语义解析结果
     * @return 带注释的DSL
     */
    public DSL annotate(DSL dsl, String query, Map<String, Object> semanticResult) {
        Map<String, Object> annotations = new HashMap<>();
        annotations.put("original_query", query);
        annotations.put("intent", semanticResult.getOrDefault("intent", "SELECT"));
        annotations.put("confidence", semanticResult.getOrDefault("confidence", 1.0));
        annotations.put("reasoning", generateReasoning(dsl, semanticResult));
        dsl.setAnnotations(annotations);
        return dsl;
    }

    /**
     * 生成DSL的推理说明文本
     */
    private String generateReasoning(DSL dsl, Map<String, Object> semanticResult) {
        List<String> parts = new ArrayList<>();
        parts.add("操作类型: " + dsl.getOperation());
        if (dsl.getTables() != null && !dsl.getTables().isEmpty()) {
            parts.add("查询表: " + String.join(", ", dsl.getTables()));
        }
        if (dsl.getFields() != null && !dsl.getFields().isEmpty()) {
            parts.add("查询字段: " + String.join(", ", dsl.getFields()));
        }
        if (dsl.getConditions() != null && !dsl.getConditions().isEmpty()) {
            List<String> condStrs = new ArrayList<>();
            for (Condition c : dsl.getConditions()) {
                condStrs.add(c.getField() + " " + c.getOperator() + " " + c.getValue());
            }
            parts.add("查询条件: " + String.join(" AND ", condStrs));
        }
        if (dsl.getAggregations() != null && !dsl.getAggregations().isEmpty()) {
            parts.add("聚合操作: " + String.join(", ", dsl.getAggregations()));
        }
        if (dsl.getGroupBy() != null && !dsl.getGroupBy().isEmpty()) {
            parts.add("分组字段: " + String.join(", ", dsl.getGroupBy()));
        }
        if (dsl.getOrderBy() != null && !dsl.getOrderBy().isEmpty()) {
            List<String> orderStrs = new ArrayList<>();
            for (Map<String, String> o : dsl.getOrderBy()) {
                orderStrs.add(o.getOrDefault("field", "") + " " + o.getOrDefault("direction", ""));
            }
            parts.add("排序: " + String.join(", ", orderStrs));
        }
        if (dsl.getJoins() != null && !dsl.getJoins().isEmpty()) {
            List<String> joinStrs = new ArrayList<>();
            for (Join j : dsl.getJoins()) {
                joinStrs.add(j.getFromTable() + "." + j.getFromField() + " = " + j.getToTable() + "." + j.getToField());
            }
            parts.add("表关联: " + String.join(" AND ", joinStrs));
        }
        return String.join("; ", parts);
    }
}
