package com.dm.cn.nldslsql.modules.llm;

import java.util.*;

/**
 * 关系推理器
 * 根据查询涉及的表名，从schema中推理出表之间的关联关系（JOIN条件）
 */
public class RelationReasoner {
    private final Map<String, Object> schema;

    public RelationReasoner(Map<String, Object> schema) {
        this.schema = schema;
    }

    /**
     * 推理多表查询所需的JOIN信息
     * @param tables 查询涉及的表名列表
     * @return JOIN信息列表，包含关联类型、源表、源字段、目标表、目标字段
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> inferJoins(List<String> tables) {
        List<Map<String, Object>> joins = new ArrayList<>();
        List<Map<String, String>> relations = (List<Map<String, String>>) schema.get("relations");
        if (relations == null) return joins;

        Set<String> tableSet = new HashSet<>(tables);
        // 查找所有两表之间都存在的关系作为JOIN条件
        for (Map<String, String> relation : relations) {
            if (tableSet.contains(relation.get("from_table")) && tableSet.contains(relation.get("to_table"))) {
                Map<String, Object> join = new HashMap<>();
                join.put("type", "INNER JOIN");
                join.put("from_table", relation.get("from_table"));
                join.put("from_field", relation.get("from_field"));
                join.put("to_table", relation.get("to_table"));
                join.put("to_field", relation.get("to_field"));
                joins.add(join);
            }
        }
        return joins;
    }

    /**
     * 推理表之间的所有关系（包括直接关系和间接关系）
     * @param tables 查询涉及的表名列表
     * @return 包含直接关联和间接关联的关系Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> inferRelationships(List<String> tables) {
        Map<String, Object> relationships = new HashMap<>();
        List<Map<String, String>> directRelations = new ArrayList<>();
        List<Map<String, String>> indirectRelations = new ArrayList<>();

        List<Map<String, String>> relations = (List<Map<String, String>>) schema.get("relations");
        if (relations != null) {
            Set<String> tableSet = new HashSet<>(tables);
            for (Map<String, String> relation : relations) {
                // 只要关系的一端在查询表中，就视为直接关联
                if (tableSet.contains(relation.get("from_table")) || tableSet.contains(relation.get("to_table"))) {
                    directRelations.add(relation);
                }
            }
        }

        relationships.put("direct_relations", directRelations);
        relationships.put("indirect_relations", indirectRelations);
        return relationships;
    }
}
