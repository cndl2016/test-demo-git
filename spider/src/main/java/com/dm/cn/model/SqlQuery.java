package com.dm.cn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 根实体类：对应整个JSON结构
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlQuery {
    /**
     * 数据库ID
     */
    private String db_id;

    /**
     * SQL查询语句
     */
    private String query;

    /**
     * SQL分词（原始）
     */
    private List<String> query_toks;

    /**
     * SQL分词（无值）
     */
    private List<String> query_toks_no_value;

    /**
     * 问题描述
     */
    private String question;

    /**
     * 答案
     */
    private String answer;

    /**
     * 问题分词
     */
    private List<String> question_toks;

    /**
     * SQL结构化对象
     */
    private SqlObject sql;
}