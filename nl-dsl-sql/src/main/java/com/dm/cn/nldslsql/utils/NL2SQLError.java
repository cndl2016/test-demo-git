package com.dm.cn.nldslsql.utils;

/**
 * NL2DSL2SQL自定义异常基类
 * 定义了各阶段的错误类型，便于错误追踪和分类处理
 */
public class NL2SQLError extends RuntimeException {
    private final String errorCode;

    public NL2SQLError(String message) {
        this(message, null);
    }

    public NL2SQLError(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** 预处理阶段异常 */
    public static class PreprocessError extends NL2SQLError {
        public PreprocessError(String message) {
            super(message, "PREPROCESS_ERROR");
        }
    }

    /** LLM调用阶段异常 */
    public static class LLMError extends NL2SQLError {
        public LLMError(String message) {
            super(message, "LLM_ERROR");
        }
    }

    /** DSL生成阶段异常 */
    public static class DSLError extends NL2SQLError {
        public DSLError(String message) {
            super(message, "DSL_ERROR");
        }
    }

    /** SQL生成阶段异常 */
    public static class SQLError extends NL2SQLError {
        public SQLError(String message) {
            super(message, "SQL_ERROR");
        }
    }

    /** 安全检查阶段异常 */
    public static class SecurityError extends NL2SQLError {
        public SecurityError(String message) {
            super(message, "SECURITY_ERROR");
        }
    }

    /** 验证阶段异常 */
    public static class ValidationError extends NL2SQLError {
        public ValidationError(String message) {
            super(message, "VALIDATION_ERROR");
        }
    }

    /** Schema相关异常 */
    public static class SchemaError extends NL2SQLError {
        public SchemaError(String message) {
            super(message, "SCHEMA_ERROR");
        }
    }
}
