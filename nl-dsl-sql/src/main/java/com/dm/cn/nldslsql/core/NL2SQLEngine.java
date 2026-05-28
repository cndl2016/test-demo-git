package com.dm.cn.nldslsql.core;

import com.dm.cn.nldslsql.config.Nl2Dsl2SqlProperties;
import com.dm.cn.nldslsql.model.DSL;
import com.dm.cn.nldslsql.model.QueryResult;
import com.dm.cn.nldslsql.modules.dslgen.DSLGenerator;
import com.dm.cn.nldslsql.modules.dslopt.DSLOptimizer;
import com.dm.cn.nldslsql.modules.llm.LLMUnderstanding;
import com.dm.cn.nldslsql.modules.postprocessor.Postprocessor;
import com.dm.cn.nldslsql.modules.preprocessor.Preprocessor;
import com.dm.cn.nldslsql.modules.sqlgen.SQLGenerator;
import com.dm.cn.nldslsql.modules.sqlgen.SQLSecurityChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * NL2SQL引擎核心服务类
 * 负责协调整个自然语言到SQL的转换流水线
 * 包含预处理、语义理解、DSL生成、DSL优化、SQL生成和后处理六个阶段
 */
@Slf4j
@Service
public class NL2SQLEngine {

    private final DatabaseSchema schema;
    private final Nl2Dsl2SqlProperties config;
    private final ChatClient chatClient;

    private final Preprocessor preprocessor;
    private final LLMUnderstanding llmUnderstanding;
    private final DSLGenerator dslGenerator;
    private final DSLOptimizer dslOptimizer;
    private final SQLGenerator sqlGenerator;
    private final Postprocessor postprocessor;

    public NL2SQLEngine(DatabaseSchema schema, Nl2Dsl2SqlProperties config, ChatClient chatClient) {
        this.schema = schema;
        this.config = config;
        this.chatClient = chatClient;

        Map<String, Object> schemaDict = schema.toDict();
        this.preprocessor = new Preprocessor(schemaDict);
        this.llmUnderstanding = new LLMUnderstanding(chatClient, schemaDict);
        this.dslGenerator = new DSLGenerator(schemaDict);
        this.dslOptimizer = new DSLOptimizer(config.getSecurity());
        this.sqlGenerator = new SQLGenerator(config.getDatabase().getDefaultDialect(), config, chatClient, schema);
        this.postprocessor = new Postprocessor(schemaDict, Map.of("format", config.getOutputFormat()));
    }

    /**
     * 处理自然语言查询，生成SQL和DSL
     * @param naturalLanguageQuery 用户输入的自然语言查询
     * @return 查询处理结果
     */
    public QueryResult process(String naturalLanguageQuery) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing query: {}", naturalLanguageQuery);

            // 阶段1: 预处理（查询标准化、意图分类、实体识别）
            Map<String, Object> preprocessed = preprocessor.process(naturalLanguageQuery);
            log.debug("Preprocessed: {}", preprocessed);

            // 阶段2: LLM语义理解（解析查询意图、提取条件、聚合等）
            Map<String, Object> semanticResult = llmUnderstanding.process(naturalLanguageQuery, preprocessed);
            log.debug("Semantic result: {}", semanticResult);

            // 阶段3: DSL生成（将语义结果转换为结构化DSL）
            DSL dsl = dslGenerator.process(naturalLanguageQuery, semanticResult);
            log.debug("Generated DSL: {}", dsl.toJson());

            // 阶段4: DSL优化（安全校验、性能优化、标准化）
            DSL optimizedDsl = dslOptimizer.process(dsl);
            log.debug("Optimized DSL: {}", optimizedDsl.toJson());

            // 阶段5: SQL生成（将DSL映射为目标数据库的SQL）
            String sql = sqlGenerator.process(optimizedDsl);
            log.info("Generated SQL: {}", sql);

            double duration = System.currentTimeMillis() - startTime;

            // 阶段6: 后处理（格式化输出、日志记录）
            Map<String, Object> resultDict = postprocessor.process(sql, optimizedDsl, naturalLanguageQuery, duration);

            QueryResult result = new QueryResult();
            result.setOriginalQuery(naturalLanguageQuery);
            result.setSql((String) resultDict.getOrDefault("sql", ""));
            result.setDslJson((String) resultDict.get("dsl"));
            result.setExplanation((String) resultDict.get("explanation"));
            result.setSuccess(true);
            result.setDuration(duration);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("preprocessed", preprocessed);
            metadata.put("semantic_result", semanticResult);
            result.setMetadata(metadata);
            return result;

        } catch (Exception e) {
            double duration = System.currentTimeMillis() - startTime;
            log.error("Error processing query: {}", e.getMessage(), e);

            Map<String, Object> resultDict = postprocessor.processError(e, naturalLanguageQuery, Map.of("stage", "unknown"));

            QueryResult result = new QueryResult();
            result.setOriginalQuery(naturalLanguageQuery);
            result.setSql("");
            result.setSuccess(false);
            result.setError((String) resultDict.getOrDefault("error", e.getMessage()));
            result.setErrorType((String) resultDict.get("error_type"));
            result.setDuration(duration);
            return result;
        }
    }

    /**
     * 获取查询的中间DSL表示（用于调试）
     * @param naturalLanguageQuery 自然语言查询
     * @return DSL对象
     */
    public DSL getIntermediateDsl(String naturalLanguageQuery) {
        try {
            Map<String, Object> preprocessed = preprocessor.process(naturalLanguageQuery);
            Map<String, Object> semanticResult = llmUnderstanding.process(naturalLanguageQuery, preprocessed);
            return dslGenerator.process(naturalLanguageQuery, semanticResult);
        } catch (Exception e) {
            log.error("Error generating intermediate DSL: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证SQL语句的安全性
     * @param sql SQL语句
     * @return 是否通过安全校验
     */
    public boolean validateQuery(String sql) {
        SQLSecurityChecker checker = new SQLSecurityChecker();
        return checker.check(sql).isValid();
    }
}
