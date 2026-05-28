package com.dm.cn.nldslsql.modules.preprocessor;

import java.util.*;

/**
 * 上下文管理器
 * 维护查询历史上下文，支持多轮对话中的表名关联
 * 默认保存最近10条查询记录
 */
public class ContextManager {
    /** 查询历史记录列表 */
    private final List<Map<String, Object>> history = new ArrayList<>();
    /** 最大历史记录数 */
    private final int maxHistory;

    public ContextManager(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public ContextManager() {
        this(10);
    }

    /**
     * 添加上下文记录
     * @param query 查询内容
     * @param intent 查询意图
     * @param tables 涉及的表名列表
     */
    public void addContext(String query, String intent, List<String> tables) {
        Map<String, Object> context = new HashMap<>();
        context.put("query", query);
        context.put("intent", intent);
        context.put("tables", tables);
        history.add(context);
        // 超过最大记录数时移除最早的记录
        if (history.size() > maxHistory) {
            history.remove(0);
        }
    }

    /**
     * 获取最近一条上下文记录
     * @return 最近的上下文Map，如果没有记录则返回null
     */
    public Map<String, Object> getLastContext() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /**
     * 获取历史记录中涉及的所有表名
     * @return 去重后的表名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRelatedTables() {
        Set<String> tables = new LinkedHashSet<>();
        for (Map<String, Object> context : history) {
            List<String> ctxTables = (List<String>) context.get("tables");
            if (ctxTables != null) {
                tables.addAll(ctxTables);
            }
        }
        return new ArrayList<>(tables);
    }
}
