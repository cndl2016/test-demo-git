package com.dm.cn.openai.tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 生成同一文本块内的上下文接近性关系（W2权重）
 */
public class ContextProximityUtil {

    /**
     * 从文本块中提取所有概念，并生成两两上下文关系
     * （注：这里简化为从已提取的语义关系中获取概念，也可单独调用LLM提取所有概念）
     */
    public List<ConceptRelation> generateProximityRelations(List<TextChunk> chunks, List<ConceptRelation> semanticRelations) {
        List<ConceptRelation> proximityRelations = new ArrayList<>();

        // 按chunkId分组，获取每个块的所有概念
        for (TextChunk chunk : chunks) {
            String chunkId = chunk.getChunkId();
            // 提取当前块的所有概念
            Set<String> conceptsInChunk = new HashSet<>();
            for (ConceptRelation rel : semanticRelations) {
                if (chunkId.equals(rel.getChunkId())) {
                    conceptsInChunk.add(rel.getNode1());
                    conceptsInChunk.add(rel.getNode2());
                }
            }
            // 生成两两组合的上下文关系
            List<String> conceptList = new ArrayList<>(conceptsInChunk);
            for (int i = 0; i < conceptList.size(); i++) {
                for (int j = i + 1; j < conceptList.size(); j++) {
                    String node1 = conceptList.get(i);
                    String node2 = conceptList.get(j);
                    proximityRelations.add(new ConceptRelation(
                            node1, node2, "上下文邻近性", 0.0, 0.5, chunkId
                    ));
                }
            }
        }
        return proximityRelations;
    }
}
