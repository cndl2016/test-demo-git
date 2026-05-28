package com.dm.cn.nldslsql.modules.dslgen;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;

import java.util.*;

/**
 * DSL语义验证器
 * 验证生成的DSL是否符合数据库schema定义和基本语义规则
 */
public class DSLSemanticValidator {
    private final Map<String, Object> schema;
    private final List<String> errors = new ArrayList<>();

    public DSLSemanticValidator(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * 验证DSL的语义正确性
     * @param dsl 待验证的DSL对象
     * @return 验证是否通过
     */
    @SuppressWarnings("unchecked")
    public boolean validate(DSL dsl) {
        errors.clear();

        // 检查是否指定了至少一个表
        if (dsl.getTables() == null || dsl.getTables().isEmpty()) {
            errors.add("DSL must specify at least one table");
        }

        // 检查表名是否在schema中存在
        List<String> availableTables = new ArrayList<>();
        Map<String, Object> tablesMap = (Map<String, Object>) schema.get("tables");
        if (tablesMap != null) {
            availableTables.addAll(tablesMap.keySet());
        }

        for (String table : dsl.getTables()) {
            if (!availableTables.contains(table)) {
                errors.add("Unknown table: " + table);
            }
        }

        // 检查条件操作符是否合法
        List<String> validOperators = Arrays.asList("=", "!=", ">", "<", ">=", "<=", "LIKE", "IN", "NOT IN", "IS NULL", "IS NOT NULL");
        if (dsl.getConditions() != null) {
            for (Condition cond : dsl.getConditions()) {
                if (!validOperators.contains(cond.getOperator())) {
                    errors.add("Invalid operator: " + cond.getOperator());
                }
            }
        }

        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }
}
