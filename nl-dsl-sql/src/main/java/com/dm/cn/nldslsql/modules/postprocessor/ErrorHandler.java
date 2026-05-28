package com.dm.cn.nldslsql.modules.postprocessor;

import java.util.*;

/**
 * 错误处理器
 * 捕获并封装异常信息，将技术异常转换为用户友好的中文提示
 * 同时维护错误日志供排查使用
 */
public class ErrorHandler {
    private final List<Map<String, Object>> errorLog = new ArrayList<>();

    /**
     * 处理异常，提取错误类型、消息和上下文
     * @param error 异常对象
     * @param context 上下文信息
     * @return 错误信息Map
     */
    public Map<String, Object> handle(Exception error, Map<String, Object> context) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("error_type", error.getClass().getSimpleName());
        errorInfo.put("error_message", error.getMessage());
        errorInfo.put("context", context);
        errorInfo.put("handled", true);
        errorLog.add(errorInfo);
        return errorInfo;
    }

    /**
     * 获取用户友好的错误提示
     * @param error 异常对象
     * @return 中文错误提示
     */
    public String getErrorMessage(Exception error) {
        String errorType = error.getClass().getSimpleName();
        Map<String, String> userFriendlyMessages = Map.of(
                "ValueError", "输入数据验证失败，请检查查询内容",
                "KeyError", "缺少必要的配置信息",
                "AttributeError", "处理过程中发生错误",
                "RuntimeError", "运行时错误，请稍后重试",
                "LLMError", "大模型调用失败，请检查网络或API配置",
                "SecurityError", "安全检查未通过"
        );
        return userFriendlyMessages.getOrDefault(errorType, error.getMessage());
    }

    public List<Map<String, Object>> getErrorLog() {
        return errorLog;
    }
}
