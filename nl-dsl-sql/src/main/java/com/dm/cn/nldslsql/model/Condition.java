package com.dm.cn.nldslsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询条件模型类
 * 用于表示SQL中的WHERE条件，包含字段名、操作符、值和逻辑关系
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    /** 条件字段名 */
    private String field;
    /** 操作符，如 =, >, <, LIKE 等 */
    private String operator;
    /** 条件值 */
    private Object value;
    /** 逻辑关系，如 AND 或 OR，默认为 AND */
    private String logical = "AND";

    /**
     * 创建条件对象（默认逻辑关系为AND）
     * @param field 字段名
     * @param operator 操作符
     * @param value 条件值
     */
    public Condition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.logical = "AND";
    }
}
