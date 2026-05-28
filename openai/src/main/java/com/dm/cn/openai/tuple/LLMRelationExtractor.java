package com.dm.cn.openai.tuple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于Spring AI调用LLM提取概念和语义关系（W1权重）
 */
public class LLMRelationExtractor {
    /**
     * 提取单个文本块的语义关系（W1权重）
     */
    public List<ConceptRelation> extractSemanticRelations(TextChunk chunk, ChatModel chatModel) {
        // 1. 构建提示词（对齐原项目Mistral 7B的提示逻辑）
//        PromptTemplate promptTemplate = new PromptTemplate("""
//                You are a network graph maker who extracts terms and their relations from a given context.
//                You are provided with a context chunk. Your task is to extract the ontology
//                of terms mentioned in the given context. These terms should represent the key concepts as per the context.
//                Think 1: Extract atomistic key terms (object, entity, location, organization, concept etc.)
//                Think 2: Extract one-on-one relations between terms (same sentence/paragraph are related)
//                Think 3: Describe the relation in 1-2 sentences
//                Format your output as a JSON array of objects, each object has: "node1", "node2", "edge"
//                Do NOT return any other text except the JSON array!
//
//                context: {chunk_content}
//                """);
//        PromptTemplate promptTemplate = new PromptTemplate("""
//                You are a network graph maker who extracts terms and their relations from a given context.
//                You are provided with a context chunk. Your task is to extract the ontology
//                of terms mentioned in the given context. These terms should represent the key concepts as per the context.
//                Think 1: Extract atomistic key terms (object, entity, location, organization, concept etc.)
//                Think 2: Extract one-on-one relations between terms (same sentence/paragraph are related)
//                Think 3: Describe the relation in one word or one sentences
//                Format your output as a JSON array of objects, each object has: "node1", "node2", "edge"
//                Do NOT return any other text except the JSON array!
//
//                context: {chunk_content}
//                """);
        PromptTemplate promptTemplate = new PromptTemplate("""
                你是一名网络图谱构建助手，负责从给定上下文中抽取术语及其相互关系。
                你会收到一个文本块，你的任务是抽取该上下文中提到的术语本体。
                这些术语应能代表文本中的核心概念。
                
                思考1：遍历每个句子时，找出其中提到的关键术语。
                术语可包括物体、实体、位置、组织、人物、
                  状态、缩写、文档、服务、概念等。
                  术语应尽可能原子化（不可再拆分）
                思考2：思考这些术语如何与其他术语形成一对一的关联。
                  出现在同一句子或同一段落中的术语通常相互关联。
                  一个术语可能与多个其他术语相关联
                思考3：找出每对相关术语之间的具体关系。
                
                请将输出格式化为JSON列表。列表中的每个元素包含一对术语及其之间的关系，格式如下：
                {jsonFormat}
                
                文本块:
                {chunk_content}
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("jsonFormat", """
                [
                   {
                       "node1": 从抽取的本体中得到的一个概念
                       "node2": 从抽取的本体中得到的另一个相关概念
                       "edge": 两个概念（node1和node2）之间的关系，用一个词或者一句话描述
                   }, {...}
                ]""");
        params.put("chunk_content", chunk.getContent());

        // 构建完整Prompt（System + User）
        Prompt prompt = promptTemplate.create(params);

        // 2. 调用LLM
        String llmOutput = chatModel.call(prompt).getResult().getOutput().getText().trim();

        // 3. 解析JSON为ConceptRelation列表（绑定chunk_id和W1权重）
        List<ConceptRelation> relations = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> rawRelations = mapper.readValue(
                    llmOutput, new TypeReference<List<Map<String, String>>>() {
                    }
            );
            for (Map<String, String> rawRel : rawRelations) {
                String node1 = rawRel.get("node1").toLowerCase().trim();
                String node2 = rawRel.get("node2").toLowerCase().trim();
                String edge = rawRel.get("edge").trim();
                // 跳过空概念
                if (node1.isEmpty() || node2.isEmpty()) {
                    continue;
                }
                relations.add(new ConceptRelation(
                        node1, node2, edge, 1.0, 0.0, chunk.getChunkId()
                ));
            }
        } catch (Exception e) {
            System.err.println("LLM输出解析失败: " + llmOutput);
            e.printStackTrace();
        }
        return relations;
    }

    /**
     * 批量提取所有文本块的语义关系
     */
    public List<ConceptRelation> batchExtractSemanticRelations(List<TextChunk> chunks, ChatModel chatModel) {
        List<ConceptRelation> allRelations = new ArrayList<>();
        for (TextChunk chunk : chunks) {
            allRelations.addAll(extractSemanticRelations(chunk, chatModel));
        }
        return allRelations;
    }
}
