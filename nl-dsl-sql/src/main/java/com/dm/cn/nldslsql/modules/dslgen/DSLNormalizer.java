package com.dm.cn.nldslsql.modules.dslgen;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;

/**
 * DSL标准化器
 * 对生成的DSL进行规范化处理，如设置默认字段、清理条件值、修正limit等
 */
public class DSLNormalizer {
    /**
     * 标准化DSL对象
     * @param dsl 待标准化的DSL
     * @return 标准化后的DSL
     */
    public DSL normalize(DSL dsl) {
        // 如果没有指定字段，默认查询所有字段
        if (dsl.getFields() == null || dsl.getFields().isEmpty()) {
            dsl.setFields(java.util.List.of("*"));
        }

        // 去除条件值两端的引号
        if (dsl.getConditions() != null) {
            for (Condition cond : dsl.getConditions()) {
                if (cond.getValue() instanceof String) {
                    String val = (String) cond.getValue();
                    cond.setValue(val.replaceAll("^['\"]|['\"]$", ""));
                }
            }
        }

        // 修正非法的limit值
        if (dsl.getLimit() != null && dsl.getLimit() < 0) {
            dsl.setLimit(null);
        }

        return dsl;
    }
}
