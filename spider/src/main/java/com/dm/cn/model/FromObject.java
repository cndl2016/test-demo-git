package com.dm.cn.model;

import lombok.Data;
import java.util.List;

/**
 * SQL from子句实体
 */
@Data
public class FromObject {
    /**
     * 表单元
     */
    private List<List<Object>> table_units;

    /**
     * 条件
     */
    private List<Object> conds;
}
