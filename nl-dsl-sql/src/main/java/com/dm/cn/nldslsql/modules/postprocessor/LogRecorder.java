package com.dm.cn.nldslsql.modules.postprocessor;

import com.dm.cn.nldslsql.model.DSL;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询日志记录器
 * 使用SLF4J记录查询处理过程和结果，支持分级日志和查询上下文记录
 * 便于问题排查和运行监控
 */
@Slf4j
public class LogRecorder {

    /**
     * 通用日志记录方法
     * @param level 日志级别 (debug/info/warn/error/critical)
     * @param message 日志消息
     * @param context 上下文信息
     */
    public void record(String level, String message, Map<String, Object> context) {
        Map<String, Object> extra = context != null ? context : new HashMap<>();
        switch (level.toLowerCase()) {
            case "debug" -> log.debug("{} - {}", message, extra);
            case "info" -> log.info("{} - {}", message, extra);
            case "warning", "warn" -> log.warn("{} - {}", message, extra);
            case "error" -> log.error("{} - {}", message, extra);
            case "critical" -> log.error("CRITICAL: {} - {}", message, extra);
            default -> log.info("{} - {}", message, extra);
        }
    }

    /**
     * 记录一次查询的处理结果
     * @param query 原始自然语言查询
     * @param sql 生成的SQL
     * @param dsl DSL对象
     * @param success 是否成功
     * @param duration 处理耗时
     */
    public void recordQuery(String query, String sql, DSL dsl, boolean success, double duration) {
        Map<String, Object> context = new HashMap<>();
        context.put("original_query", query);
        context.put("generated_sql", sql);
        context.put("dsl", dsl != null ? dsl.toJson() : null);
        context.put("success", success);
        context.put("duration", duration);

        String level = success ? "info" : "error";
        String message = "Query processed: " + (success ? "success" : "failed");
        record(level, message, context);
    }
}
