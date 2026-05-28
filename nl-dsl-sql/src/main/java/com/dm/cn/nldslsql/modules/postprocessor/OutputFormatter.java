package com.dm.cn.nldslsql.modules.postprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

/**
 * 输出格式化器
 * 将结果Map格式化为不同输出形式：JSON、Markdown或纯文本
 * 支持根据配置自动选择输出格式
 */
public class OutputFormatter {
    private final String formatType;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutputFormatter(String formatType) {
        this.formatType = formatType != null ? formatType : "plain";
        // 启用JSON美化输出
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 根据配置的格式类型格式化结果
     * @param result 结果Map
     * @return 格式化后的字符串
     */
    public String format(Map<String, Object> result) {
        return switch (formatType) {
            case "json" -> formatJson(result);
            case "markdown" -> formatMarkdown(result);
            default -> formatPlain(result);
        };
    }

    /**
     * 格式化为JSON字符串
     */
    private String formatJson(Map<String, Object> result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return result.toString();
        }
    }

    /**
     * 格式化为Markdown文档
     */
    private String formatMarkdown(Map<String, Object> result) {
        List<String> lines = new ArrayList<>();
        lines.add("## 查询结果");
        lines.add("");

        if (result.containsKey("sql")) {
            lines.add("### SQL");
            lines.add("```sql");
            lines.add(String.valueOf(result.get("sql")));
            lines.add("```");
            lines.add("");
        }

        if (result.containsKey("dsl")) {
            lines.add("### DSL");
            lines.add("```json");
            lines.add(String.valueOf(result.get("dsl")));
            lines.add("```");
            lines.add("");
        }

        if (result.containsKey("explanation")) {
            lines.add("### 解释");
            lines.add(String.valueOf(result.get("explanation")));
            lines.add("");
        }

        if (result.containsKey("error")) {
            lines.add("### 错误");
            lines.add("```");
            lines.add(String.valueOf(result.get("error")));
            lines.add("```");
            lines.add("");
        }

        return String.join("\n", lines);
    }

    /**
     * 格式化为纯文本
     */
    private String formatPlain(Map<String, Object> result) {
        List<String> lines = new ArrayList<>();

        if (result.containsKey("sql")) {
            lines.add("SQL:");
            lines.add(String.valueOf(result.get("sql")));
            lines.add("");
        }

        if (result.containsKey("explanation")) {
            lines.add("解释:");
            lines.add(String.valueOf(result.get("explanation")));
            lines.add("");
        }

        if (result.containsKey("error")) {
            lines.add("错误:");
            lines.add(String.valueOf(result.get("error")));
            lines.add("");
        }

        return String.join("\n", lines);
    }
}
