package com.dm.cn.nldslsql.modules.sqlgen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL安全检查器
 * 使用正则表达式检测SQL中的危险语句和注释注入
 * 是SQL生成后的最后一道安全防线
 */
public class SQLSecurityChecker {
    private final List<PatternMessage> dangerousPatterns = List.of(
            new PatternMessage(Pattern.compile("(?i)\\bDROP\\b"), "DROP statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bDELETE\\b"), "DELETE statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bTRUNCATE\\b"), "TRUNCATE statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bALTER\\b"), "ALTER statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bGRANT\\b"), "GRANT statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bREVOKE\\b"), "REVOKE statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bEXEC\\b"), "EXEC statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)\\bEXECUTE\\b"), "EXECUTE statement is not allowed"),
            new PatternMessage(Pattern.compile("(?i)--"), "Inline comment detected"),
            new PatternMessage(Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL), "Block comment detected")
    );

    /**
     * 检查SQL是否包含危险模式
     * @param sql SQL语句
     * @return 验证结果，包含是否通过及错误信息列表
     */
    public ValidationResult check(String sql) {
        List<String> errors = new ArrayList<>();
        for (PatternMessage pm : dangerousPatterns) {
            if (pm.pattern().matcher(sql).find()) {
                errors.add(pm.message());
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(boolean isValid, List<String> errors) {}
    private record PatternMessage(Pattern pattern, String message) {}
}
