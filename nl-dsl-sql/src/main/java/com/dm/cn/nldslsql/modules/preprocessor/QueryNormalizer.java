package com.dm.cn.nldslsql.modules.preprocessor;

import java.util.regex.Pattern;

/**
 * 查询标准化器
 * 对原始查询进行清洗和标准化处理，去除特殊字符、规范化空格、统一SQL关键字大小写
 */
public class QueryNormalizer {
    /** 匹配非字母数字、空格和中文字符的特殊字符 */
    private final Pattern specialCharsPattern = Pattern.compile("[^\\w\\s\\u4e00-\\u9fff]");
    /** 匹配多个连续空格 */
    private final Pattern multipleSpacesPattern = Pattern.compile("\\s+");
    /** SQL关键字列表，用于统一转换为小写 */
    private static final String[] KEYWORDS = {
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "LIKE", "ORDER BY", "GROUP BY",
            "HAVING", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AS", "DISTINCT",
            "COUNT", "SUM", "AVG", "MAX", "MIN"
    };

    /**
     * 标准化查询字符串
     * @param query 原始查询
     * @return 去除特殊字符、合并多余空格后的查询
     */
    public String normalize(String query) {
        query = query.trim();
        query = specialCharsPattern.matcher(query).replaceAll(" ");
        query = multipleSpacesPattern.matcher(query).replaceAll(" ");
        return query;
    }

    /**
     * 将SQL关键字统一转换为小写
     * @param query 查询字符串
     * @return 关键字小写化后的查询
     */
    public String toLowercaseKeywords(String query) {
        String result = query;
        for (String keyword : KEYWORDS) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(keyword) + "\\b", keyword.toLowerCase());
        }
        return result;
    }
}
