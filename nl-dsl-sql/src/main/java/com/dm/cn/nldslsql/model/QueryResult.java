package com.dm.cn.nldslsql.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询结果模型类
 * 封装自然语言查询处理后的完整结果，包括生成的SQL、DSL、解释信息和元数据
 */
@Data
public class QueryResult {
    /** 原始自然语言查询 */
    private String originalQuery;
    /** 生成的SQL语句 */
    private String sql;
    /** DSL的JSON表示 */
    private String dslJson;
    /** 查询解释说明 */
    private String explanation;
    /** 处理是否成功 */
    private boolean success = true;
    /** 错误信息（处理失败时） */
    private String error;
    /** 错误类型 */
    private String errorType;
    /** 处理耗时（毫秒） */
    private double duration;
    /** 验证错误列表 */
    private List<String> validationErrors = new ArrayList<>();
    /** 附加元数据，包含预处理结果和语义解析结果等 */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 将结果对象序列化为格式化的JSON字符串
     * @return 格式化的JSON字符串
     */
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize QueryResult to JSON", e);
        }
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("QueryResult(success=True, sql='%s', duration=%.2fms)", sql, duration);
        } else {
            return String.format("QueryResult(success=False, error='%s')", error);
        }
    }
}
