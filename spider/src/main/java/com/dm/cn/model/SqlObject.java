package com.dm.cn.model;

import lombok.Data;
import java.util.List;

/**
 * SQL结构化对象
 */
@Data
public class SqlObject {
    /**
     * from子句
     */
    private FromObject from;

    /**
     * select子句
     */
    private List<Object> select;

    /**
     * where条件
     */
    private List<Object> where;

    /**
     * groupBy子句
     */
    private List<Object> groupBy;

    /**
     * having子句
     */
    private List<Object> having;

    /**
     * orderBy子句
     */
    private List<Object> orderBy;

    /**
     * 限制条数
     */
    private Object limit;

    /**
     * 交集
     */
    private Object intersect;

    /**
     * 并集
     */
    private Object union;

    /**
     * 差集
     */
    private Object except;
}