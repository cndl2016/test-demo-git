package com.dm.cn.nldslsql.modules.postprocessor;

import com.dm.cn.nldslsql.model.DSL;

import java.util.*;

/**
 * 后置处理器
 * NL2SQL流水线的最后阶段，负责结果组装、验证、格式化、日志记录和错误处理
 * 将SQL、DSL、解释等信息封装为统一的响应结构
 */
public class Postprocessor {
    private final Map<String, Object> schema;
    private final Map<String, Object> config;
    private final ResultValidator resultValidator;
    private final OutputFormatter formatter;
    private final ErrorHandler errorHandler;
    private final LogRecorder logRecorder;

    public Postprocessor(Map<String, Object> schema, Map<String, Object> config) {
        this.schema = schema != null ? schema : new HashMap<>();
        this.config = config != null ? config : new HashMap<>();
        this.resultValidator = new ResultValidator(schema);
        String formatType = (String) this.config.getOrDefault("format", "plain");
        this.formatter = new OutputFormatter(formatType);
        this.errorHandler = new ErrorHandler();
        this.logRecorder = new LogRecorder();
    }

    /**
     * 处理成功结果：组装SQL、DSL、解释、验证信息和格式化输出
     * @param sql 生成的SQL
     * @param dsl 优化后的DSL
     * @param query 原始查询
     * @param duration 处理耗时（秒）
     * @return 结果Map
     */
    public Map<String, Object> process(String sql, DSL dsl, String query, double duration) {
        Map<String, Object> result = new HashMap<>();
        result.put("sql", sql);
        result.put("dsl", dsl != null ? dsl.toJson() : null);
        result.put("success", true);

        // 结果验证：检查SQL是否引用了未知表、是否有基本语法错误
        ResultValidator.ValidationResult validation = resultValidator.validate(sql, dsl);
        if (!validation.valid()) {
            result.put("validation_errors", validation.errors());
        }

        // 提取DSL中的推理说明作为解释
        if (dsl != null && dsl.getAnnotations() != null) {
            result.put("explanation", dsl.getAnnotations().get("reasoning"));
        }

        result.put("formatted", formatter.format(result));
        logRecorder.recordQuery(query, sql, dsl, true, duration);
        return result;
    }

    /**
     * 处理错误结果：封装错误信息、友好提示和格式化输出
     * @param error 异常对象
     * @param query 原始查询
     * @param context 上下文信息
     * @return 错误结果Map
     */
    public Map<String, Object> processError(Exception error, String query, Map<String, Object> context) {
        Map<String, Object> errorInfo = errorHandler.handle(error, context != null ? context : new HashMap<>());

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorHandler.getErrorMessage(error));
        result.put("error_type", errorInfo.get("error_type"));

        Map<String, Object> formatInput = new HashMap<>();
        formatInput.put("error", errorInfo.get("error_message"));
        formatInput.put("query", query);
        result.put("formatted", formatter.format(formatInput));

        logRecorder.recordQuery(query, "", null, false, 0);
        return result;
    }
}
