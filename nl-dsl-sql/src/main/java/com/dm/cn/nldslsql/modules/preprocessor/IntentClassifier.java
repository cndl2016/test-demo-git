package com.dm.cn.nldslsql.modules.preprocessor;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 意图分类器
 * 根据查询中的关键词判断用户的操作意图（SELECT/INSERT/UPDATE/DELETE）
 * 默认返回SELECT，确保安全性
 */
public class IntentClassifier {
    private final Map<String, List<String>> intentPatterns = new HashMap<>();

    public IntentClassifier() {
        // 初始化各类意图的关键词映射
        intentPatterns.put("SELECT", Arrays.asList("查询", "查找", "获取", "select", "找", "看看", "列出", "统计", "count", "sum", "avg", "max", "min"));
        intentPatterns.put("INSERT", Arrays.asList("添加", "插入", "新增", "insert", "创建", "加入"));
        intentPatterns.put("UPDATE", Arrays.asList("更新", "修改", "改变", "update", "编辑", "调整"));
        intentPatterns.put("DELETE", Arrays.asList("删除", "移除", "delete", "清除"));
    }

    /**
     * 分类查询意图
     * @param query 查询字符串
     * @return 意图类型，如 SELECT/INSERT/UPDATE/DELETE，默认返回SELECT
     */
    public String classify(String query) {
        String queryLower = query.toLowerCase();
        Map<String, Integer> scores = new HashMap<>();

        // 统计各类意图关键词的匹配数量
        for (Map.Entry<String, List<String>> entry : intentPatterns.entrySet()) {
            int score = 0;
            for (String pattern : entry.getValue()) {
                if (Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE).matcher(queryLower).find()) {
                    score++;
                }
            }
            scores.put(entry.getKey(), score);
        }

        // 如果没有匹配到任何关键词，默认返回SELECT
        if (scores.isEmpty() || scores.values().stream().max(Integer::compare).orElse(0) == 0) {
            return "SELECT";
        }

        // 返回得分最高的意图
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("SELECT");
    }
}
