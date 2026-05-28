package com.dm.cn.nldslsql.modules.dslopt;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;

import java.util.*;

/**
 * DSL安全验证器
 * 验证DSL是否符合安全策略，防止SQL注入和危险操作
 */
public class SecurityValidator {
    private final List<String> allowedOperations;
    private final int maxQueryDepth;
    /** 危险关键字列表 */
    private final List<String> dangerousKeywords = Arrays.asList("DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE");

    public SecurityValidator(Map<String, Object> config) {
        this.allowedOperations = (List<String>) config.getOrDefault("allowed_operations", List.of("SELECT"));
        this.maxQueryDepth = (int) config.getOrDefault("max_query_depth", 10);
    }

    /**
     * 验证DSL的安全性
     * @param dsl 待验证的DSL
     * @return 验证结果
     */
    public ValidationResult validate(DSL dsl) {
        List<String> errors = new ArrayList<>();

        // 检查操作类型是否在允许列表中
        if (!allowedOperations.contains(dsl.getOperation())) {
            errors.add("Operation " + dsl.getOperation() + " is not allowed");
        }

        // 检查查询深度是否超过限制
        if (dsl.getTables().size() > maxQueryDepth) {
            errors.add("Query depth " + dsl.getTables().size() + " exceeds maximum " + maxQueryDepth);
        }

        // 检查条件中是否包含危险关键字
        for (Condition cond : dsl.getConditions()) {
            for (String keyword : dangerousKeywords) {
                if (cond.getOperator().toUpperCase().contains(keyword)) {
                    errors.add("Dangerous keyword detected: " + keyword);
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 清理值中的危险字符，防止SQL注入
     * @param value 原始值
     * @return 清理后的值
     */
    public String sanitizeValue(String value) {
        String[] dangerousPatterns = {"'", "\"", ";", "--", "/*", "*/", "xp_", "sp_"};
        String sanitized = value;
        for (String pattern : dangerousPatterns) {
            sanitized = sanitized.replace(pattern, "");
        }
        return sanitized;
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}
