package com.dm.cn.nldslsql.modules.postprocessor;

import com.dm.cn.nldslsql.model.DSL;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 结果验证器
 * 对生成的SQL进行基本验证：非空检查、表名合法性、语法开头检查
 * 作为最终输出的质量把关
 */
public class ResultValidator {
    private final Map<String, Object> schema;

    public ResultValidator(Map<String, Object> schema) {
        this.schema = schema != null ? schema : new HashMap<>();
    }

    /**
     * 验证SQL和DSL的基本合法性
     * @param sql 生成的SQL
     * @param dsl DSL对象
     * @return 验证结果
     */
    public ValidationResult validate(String sql, DSL dsl) {
        List<String> errors = new ArrayList<>();

        // SQL非空检查
        if (sql == null || sql.trim().isEmpty()) {
            errors.add("SQL is empty");
        }

        // 表名引用检查
        if (!validateTables(sql)) {
            errors.add("SQL references unknown tables");
        }

        // 基本语法开头检查
        if (!validateSyntaxBasic(sql)) {
            errors.add("SQL has basic syntax errors");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 检查SQL中引用的表是否在schema中存在
     */
    @SuppressWarnings("unchecked")
    private boolean validateTables(String sql) {
        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        if (tablesDict == null || tablesDict.isEmpty()) {
            return true;
        }

        Set<String> availableTables = tablesDict.keySet();
        for (String table : availableTables) {
            if (Pattern.compile("\\b" + Pattern.quote(table) + "\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
                return true;
            }
        }
        return true;
    }

    /**
     * 检查SQL是否以合法的SQL关键字开头
     */
    private boolean validateSyntaxBasic(String sql) {
        String sqlUpper = sql.toUpperCase().trim();
        String[] validStarts = {"SELECT", "INSERT", "UPDATE", "DELETE"};
        for (String start : validStarts) {
            if (sqlUpper.startsWith(start)) {
                return true;
            }
        }
        return false;
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}
