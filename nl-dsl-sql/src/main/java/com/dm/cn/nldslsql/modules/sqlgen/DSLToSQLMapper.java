package com.dm.cn.nldslsql.modules.sqlgen;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.Join;

import java.util.*;

/**
 * DSL到SQL的映射器
 * 基于规则将DSL对象转换为标准SQL查询语句，不依赖LLM
 * 支持SELECT语句的完整构造，包括字段、JOIN、WHERE、GROUP BY、ORDER BY、LIMIT
 */
public class DSLToSQLMapper {
    private final String dialect;

    public DSLToSQLMapper(String dialect) {
        this.dialect = dialect;
    }

    /**
     * 将DSL映射为SQL语句
     * @param dsl DSL对象
     * @return SQL查询语句
     */
    public String map(DSL dsl) {
        if ("SELECT".equalsIgnoreCase(dsl.getOperation())) {
            return mapSelect(dsl);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + dsl.getOperation());
        }
    }

    /**
     * 构建SELECT语句
     */
    private String mapSelect(DSL dsl) {
        List<String> parts = new ArrayList<>();

        parts.add("SELECT");

        // 处理聚合字段或普通字段
        if (dsl.getAggregations() != null && !dsl.getAggregations().isEmpty()) {
            List<String> aggFields = new ArrayList<>();
            for (String agg : dsl.getAggregations()) {
                if ("COUNT".equalsIgnoreCase(agg)) {
                    aggFields.add("COUNT(*)");
                } else {
                    aggFields.add(agg + "(*)");
                }
            }
            parts.add(String.join(", ", aggFields));
        } else {
            if (dsl.getFields() != null && !dsl.getFields().isEmpty()) {
                parts.add(String.join(", ", dsl.getFields()));
            } else {
                parts.add("*");
            }
        }

        // FROM子句：单表直接列出，多表有JOIN时只列主表
        parts.add("FROM");
        if (dsl.getJoins() != null && !dsl.getJoins().isEmpty() && dsl.getTables() != null && !dsl.getTables().isEmpty()) {
            parts.add(dsl.getTables().get(0));
        } else {
            parts.add(String.join(", ", dsl.getTables()));
        }

        // JOIN子句：智能判断JOIN目标表，避免主表重复JOIN
        if (dsl.getJoins() != null) {
            String primaryTable = dsl.getTables().get(0);
            for (Join join : dsl.getJoins()) {
                // 若toTable就是主表，则应使用fromTable作为JOIN目标，避免自连接错误
                String joinTarget = join.getToTable().equals(primaryTable)
                        ? join.getFromTable() : join.getToTable();
                String joinSql = join.getJoinType() + " " + joinTarget + " ON "
                        + join.getFromTable() + "." + join.getFromField() + " = "
                        + join.getToTable() + "." + join.getToField();
                parts.add(joinSql);
            }
        }

        // WHERE子句：拼接条件和逻辑运算符
        if (dsl.getConditions() != null && !dsl.getConditions().isEmpty()) {
            List<String> whereParts = new ArrayList<>();
            for (int i = 0; i < dsl.getConditions().size(); i++) {
                Condition cond = dsl.getConditions().get(i);
                if (i > 0) {
                    whereParts.add(cond.getLogical());
                }
                String value = formatValue(cond.getValue(), cond.getOperator());
                whereParts.add(cond.getField() + " " + cond.getOperator() + " " + value);
            }
            parts.add("WHERE");
            parts.add(String.join(" ", whereParts));
        }

        // GROUP BY子句
        if (dsl.getGroupBy() != null && !dsl.getGroupBy().isEmpty()) {
            parts.add("GROUP BY");
            parts.add(String.join(", ", dsl.getGroupBy()));
        }

        // ORDER BY子句
        if (dsl.getOrderBy() != null && !dsl.getOrderBy().isEmpty()) {
            List<String> orderParts = new ArrayList<>();
            for (Map<String, String> order : dsl.getOrderBy()) {
                String field = order.getOrDefault("field", "");
                String direction = order.getOrDefault("direction", "ASC");
                orderParts.add(field + " " + direction);
            }
            if (!orderParts.isEmpty()) {
                parts.add("ORDER BY");
                parts.add(String.join(", ", orderParts));
            }
        }

        // LIMIT子句
        if (dsl.getLimit() != null) {
            parts.add("LIMIT " + dsl.getLimit());
        }

        return String.join(" ", parts);
    }

    /**
     * 格式化条件值：字符串加引号，LIKE操作符添加通配符，null转为NULL
     */
    private String formatValue(Object value, String operator) {
        if ("LIKE".equalsIgnoreCase(operator) && value instanceof String) {
            return "'%" + value + "%'";
        } else if (value instanceof String) {
            return "'" + value + "'";
        } else if (value == null) {
            return "NULL";
        } else {
            return String.valueOf(value);
        }
    }
}
