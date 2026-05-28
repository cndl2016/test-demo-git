package com.dm.cn.nldslsql.modules.dslopt;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.Join;

import java.util.*;

/**
 * DSL性能优化器
 * 对DSL进行去重和字段优化，减少冗余JOIN、重复条件和不必要的通配符查询
 * 提升生成SQL的执行效率
 */
public class PerformanceOptimizer {
    /**
     * 执行性能优化：去重JOIN、优化字段、去重条件
     * @param dsl 待优化的DSL
     * @return 优化后的DSL
     */
    public DSL optimize(DSL dsl) {
        dsl = optimizeJoins(dsl);
        dsl = optimizeFields(dsl);
        dsl = optimizeConditions(dsl);
        return dsl;
    }

    /**
     * JOIN去重：基于五元组(fromTable, fromField, toTable, toField)去重相同关联
     */
    private DSL optimizeJoins(DSL dsl) {
        Set<String> seen = new HashSet<>();
        List<Join> uniqueJoins = new ArrayList<>();
        for (Join join : dsl.getJoins()) {
            String key = join.getFromTable() + ":" + join.getFromField() + ":" + join.getToTable() + ":" + join.getToField();
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueJoins.add(join);
            }
        }
        dsl.setJoins(uniqueJoins);
        return dsl;
    }

    /**
     * 字段优化：多表查询时将"*"展开为各表的"table.*"，单表时保持"*"
     */
    private DSL optimizeFields(DSL dsl) {
        if (dsl.getFields().contains("*") && (dsl.getAggregations() == null || dsl.getAggregations().isEmpty())) {
            List<String> tableFields = new ArrayList<>();
            for (String table : dsl.getTables()) {
                tableFields.add(table + ".*");
            }
            if (dsl.getTables().size() == 1) {
                dsl.setFields(List.of("*"));
            } else {
                dsl.setFields(tableFields);
            }
        }
        return dsl;
    }

    /**
     * 条件去重：基于(field, operator, value)三元组去重相同条件
     */
    private DSL optimizeConditions(DSL dsl) {
        List<Condition> uniqueConditions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Condition cond : dsl.getConditions()) {
            String key = cond.getField() + ":" + cond.getOperator() + ":" + cond.getValue();
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueConditions.add(cond);
            }
        }
        dsl.setConditions(uniqueConditions);
        return dsl;
    }
}
