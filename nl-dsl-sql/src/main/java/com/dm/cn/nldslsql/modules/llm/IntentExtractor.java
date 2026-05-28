package com.dm.cn.nldslsql.modules.llm;

import java.util.Map;

/**
 * 意图提取器
 * 从预处理结果中提取查询意图
 * 当前版本固定返回SELECT以确保安全性
 */
public class IntentExtractor {
    /**
     * 提取查询意图
     * @param query 查询字符串
     * @param preprocessed 预处理结果
     * @return 意图类型，当前固定返回SELECT
     */
    public String extract(String query, Map<String, Object> preprocessed) {
        return "SELECT";
    }
}
