package com.dm.cn.nldslsql.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表关联模型类
 * 用于表示SQL中的JOIN操作，包含关联类型、源表、目标表及关联字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Join {
    /** 关联类型，如 INNER JOIN, LEFT JOIN 等 */
    private String joinType;
    /** 源表名 */
    private String fromTable;
    /** 源表关联字段 */
    private String fromField;
    /** 目标表名 */
    private String toTable;
    /** 目标表关联字段 */
    private String toField;
}
