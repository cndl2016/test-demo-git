package com.dm.cn.nldslsql.modules.dslopt;

import com.dm.cn.nldslsql.model.Condition;
import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.Join;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * DSL标准化器
 * 对DSL中的字符串元素进行统一格式化：去空格、大写操作符、去重等
 * 确保DSL格式一致性，便于后续SQL生成
 */
public class DSLStandardizer {
    /**
     * 标准化DSL对象
     * @param dsl 待标准化的DSL
     * @return 标准化后的DSL
     */
    public DSL standardize(DSL dsl) {
        // 操作类型统一为大写
        dsl.setOperation(dsl.getOperation().toUpperCase());

        // 表名去重并去除首尾空格，保持原有顺序
        List<String> tables = new ArrayList<>();
        for (String t : dsl.getTables()) {
            tables.add(t.trim());
        }
        dsl.setTables(new ArrayList<>(new LinkedHashSet<>(tables)));

        // 字段去重并去除首尾空格，保持原有顺序
        List<String> fields = new ArrayList<>();
        for (String f : dsl.getFields()) {
            fields.add(f.trim());
        }
        dsl.setFields(new ArrayList<>(new LinkedHashSet<>(fields)));

        // 分组字段去除首尾空格
        if (dsl.getGroupBy() != null) {
            List<String> groups = new ArrayList<>();
            for (String g : dsl.getGroupBy()) {
                groups.add(g.trim());
            }
            dsl.setGroupBy(groups);
        }

        // 条件字段去空格，操作符统一大写
        for (Condition cond : dsl.getConditions()) {
            cond.setField(cond.getField().trim());
            cond.setOperator(cond.getOperator().toUpperCase());
        }

        // JOIN类型统一大写
        for (Join join : dsl.getJoins()) {
            join.setJoinType(join.getJoinType().toUpperCase());
        }

        return dsl;
    }
}
