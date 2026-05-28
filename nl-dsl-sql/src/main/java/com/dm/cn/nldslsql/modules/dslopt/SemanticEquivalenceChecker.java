package com.dm.cn.nldslsql.modules.dslopt;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;

import java.util.*;

/**
 * DSL语义等价性检查器
 * 比较两个DSL对象是否在语义上等价（忽略顺序差异）
 * 用于验证优化前后的DSL是否保持语义一致性
 */
public class SemanticEquivalenceChecker {
    /**
     * 检查两个DSL是否在语义上等价
     * @param dsl1 第一个DSL对象
     * @param dsl2 第二个DSL对象
     * @return 若语义等价则返回true
     */
    public boolean check(DSL dsl1, DSL dsl2) {
        // 比较操作类型
        if (!Objects.equals(dsl1.getOperation(), dsl2.getOperation())) {
            return false;
        }
        // 比较涉及的表（忽略顺序）
        if (!new HashSet<>(dsl1.getTables()).equals(new HashSet<>(dsl2.getTables()))) {
            return false;
        }
        // 比较查询字段（忽略顺序）
        if (!new HashSet<>(dsl1.getFields()).equals(new HashSet<>(dsl2.getFields()))) {
            return false;
        }
        // 比较查询条件（规范化后比较，忽略顺序）
        if (!normalizeConditions(dsl1.getConditions()).equals(normalizeConditions(dsl2.getConditions()))) {
            return false;
        }
        return true;
    }

    /**
     * 将条件列表规范化，转换为可比较的Map列表并按字段名排序
     */
    private List<Map<String, String>> normalizeConditions(List<Condition> conditions) {
        List<Map<String, String>> normalized = new ArrayList<>();
        for (Condition cond : conditions) {
            Map<String, String> map = new HashMap<>();
            map.put("field", cond.getField());
            map.put("operator", cond.getOperator());
            map.put("value", String.valueOf(cond.getValue()));
            normalized.add(map);
        }
        // 按字段名排序，确保顺序无关的比较
        normalized.sort(Comparator.comparing(m -> m.get("field")));
        return normalized;
    }
}
