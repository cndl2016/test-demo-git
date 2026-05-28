package com.dm.cn.nldslsql.modules.dslgen;

import com.dm.cn.nldslsql.model.DSL;

import java.util.Map;

/**
 * DSL生成器
 * 负责将语义解析结果转换为结构化的DSL对象，并经过校验、标准化和注释处理
 * 是NL2SQL流水线的第三阶段核心组件
 */
public class DSLGenerator {
    private final Map<String, Object> schema;
    private final DSLBuilder builder;
    private final DSLSemanticValidator validator;
    private final DSLNormalizer normalizer;
    private final DSLAnnotator annotator;

    public DSLGenerator(Map<String, Object> schema) {
        this.schema = schema;
        this.builder = new DSLBuilder(schema);
        this.validator = new DSLSemanticValidator(schema);
        this.normalizer = new DSLNormalizer();
        this.annotator = new DSLAnnotator();
    }

    /**
     * 处理DSL生成流程：构建 -> 校验 -> 标准化 -> 注释
     * @param query 原始自然语言查询
     * @param semanticResult 语义解析结果
     * @return 完整的DSL对象
     */
    public DSL process(String query, Map<String, Object> semanticResult) {
        // 根据语义结果构建DSL对象
        DSL dsl = builder.build(semanticResult);

        // 语义校验：检查表、字段是否在schema中存在
        boolean isValid = validator.validate(dsl);
        if (!isValid) {
            throw new IllegalArgumentException("DSL validation failed: " + validator.getErrors());
        }

        // 标准化处理：默认值、清理引号、修正limit等
        dsl = normalizer.normalize(dsl);
        // 添加注释信息：原始查询、意图、置信度、推理说明
        dsl = annotator.annotate(dsl, query, semanticResult);
        return dsl;
    }
}
