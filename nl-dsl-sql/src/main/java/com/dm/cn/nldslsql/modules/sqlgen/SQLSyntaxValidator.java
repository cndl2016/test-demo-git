package com.dm.cn.nldslsql.modules.sqlgen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL语法验证器
 * 验证生成的SQL是否符合安全规范，只允许SELECT查询
 */
public class SQLSyntaxValidator {
    /**
     * 验证SQL语句
     * @param sql SQL语句
     * @return 验证结果
     */
    public ValidationResult validate(String sql) {
        List<String> errors = new ArrayList<>();
        String sqlUpper = sql.toUpperCase().trim();

        // 必须以SELECT开头
        if (!sqlUpper.startsWith("SELECT")) {
            errors.add("Only SELECT queries are allowed");
            return new ValidationResult(false, errors);
        }

        // 检查是否包含危险关键字
        String[] keywords = {"DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE", "GRANT", "REVOKE"};
        for (String keyword : keywords) {
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(sqlUpper).find()) {
                errors.add(keyword + " is not allowed");
            }
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors);
        }

        return new ValidationResult(true, errors);
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}
