package com.dm.cn.openai.tuple;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 聚合同一概念对的所有关系：求和权重 + 拼接关系描述
 */
public class RelationAggregationUtil {

    /**
     * 生成概念对的字符串键（固定顺序，避免A-B和B-A冲突）
     */
    private String getConceptPairKey(String node1, String node2) {
        // 固定顺序：字典序小的在前
        String first = node1.compareTo(node2) < 0 ? node1 : node2;
        String second = node1.compareTo(node2) < 0 ? node2 : node1;
        // 用特殊分隔符（如###）避免"a-b"和"ab-"这类拼接冲突
        return first + "###" + second;
    }

    /**
     * 聚合语义关系+上下文关系，生成最终知识图谱边
     */
    public List<AggregatedRelation> aggregateRelations(List<ConceptRelation> allRelations) {
        Map<String, List<ConceptRelation>> relationMap = new HashMap<>();

        // 1. 分组存储
        for (ConceptRelation rel : allRelations) {
            String key = getConceptPairKey(rel.getNode1(), rel.getNode2());
            relationMap.computeIfAbsent(key, k -> new ArrayList<>()).add(rel);
        }

        // 2. 聚合逻辑
        List<AggregatedRelation> aggregatedRelations = new ArrayList<>();
        for (Map.Entry<String, List<ConceptRelation>> entry : relationMap.entrySet()) {
            // 拆分键为node1和node2
            String[] nodes = entry.getKey().split("###");
            String node1 = nodes[0];
            String node2 = nodes[1];
            List<ConceptRelation> samePairRels = entry.getValue();

            // 求和权重 + 拼接关系
            double totalWeight = samePairRels.stream()
                    .mapToDouble(rel -> rel.getWeightW1() + rel.getWeightW2())
                    .sum();
            List<String> edges = samePairRels.stream()
                    .map(ConceptRelation::getEdge)
                    .distinct()
                    .collect(Collectors.toList());

            aggregatedRelations.add(new AggregatedRelation(node1, node2, totalWeight, edges));
        }
        return aggregatedRelations;
    }
}
