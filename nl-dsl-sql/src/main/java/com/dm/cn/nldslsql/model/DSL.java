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
 * DSL（领域特定语言）模型类
 * 作为自然语言查询到SQL之间的中间表示层
 * 包含查询操作类型、目标表、字段、条件、关联、分组、排序、限制等完整查询语义
 */
@Data
public class DSL {
    /** 操作类型，如 SELECT */
    private String operation;
    /** 查询涉及的表名列表 */
    private List<String> tables = new ArrayList<>();
    /** 查询字段列表 */
    private List<String> fields = new ArrayList<>();
    /** 查询条件列表 */
    private List<Condition> conditions = new ArrayList<>();
    /** 表关联信息列表 */
    private List<Join> joins = new ArrayList<>();
    /** 分组字段列表 */
    private List<String> groupBy = new ArrayList<>();
    /** 排序规则列表，每个元素包含field和direction */
    private List<Map<String, String>> orderBy = new ArrayList<>();
    /** 查询结果限制数量 */
    private Integer limit;
    /** 聚合函数列表，如 COUNT, SUM, AVG 等 */
    private List<String> aggregations = new ArrayList<>();
    /** 附加注释信息，如原始查询、置信度、推理过程等 */
    private Map<String, Object> annotations = new HashMap<>();

    /**
     * 将DSL对象序列化为格式化的JSON字符串
     * @return 格式化的JSON字符串
     */
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize DSL to JSON", e);
        }
    }
}
