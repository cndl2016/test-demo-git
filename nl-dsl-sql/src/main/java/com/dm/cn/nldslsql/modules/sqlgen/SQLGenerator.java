package com.dm.cn.nldslsql.modules.sqlgen;

import com.dm.cn.nldslsql.config.Nl2Dsl2SqlProperties;
import com.dm.cn.nldslsql.core.DatabaseSchema;
import com.dm.cn.nldslsql.model.DSL;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * SQL生成器
 * 将优化后的DSL转换为最终的SQL查询语句，支持LLM生成和规则映射两种模式
 * 包含安全检查、语法校验和性能优化等后处理步骤
 */
@Slf4j
public class SQLGenerator {
    private final String dialect;
    private final Nl2Dsl2SqlProperties config;
    private final DSLToSQLMapper mapper;
    private final ChatClient chatClient;
    private final SQLSyntaxValidator syntaxValidator;
    private final SQLPerformanceOptimizer performanceOptimizer;
    private final SQLSecurityChecker securityChecker;
    private final DatabaseSchema schema;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SQLGenerator(String dialect, Nl2Dsl2SqlProperties config, ChatClient chatClient, DatabaseSchema schema) {
        this.dialect = dialect;
        this.config = config;
        this.mapper = new DSLToSQLMapper(dialect);
        this.chatClient = chatClient;
        this.syntaxValidator = new SQLSyntaxValidator();
        this.performanceOptimizer = new SQLPerformanceOptimizer();
        this.securityChecker = new SQLSecurityChecker();
        this.schema = schema;
    }

    /**
     * 处理SQL生成流程：LLM生成/规则映射 -> 安全检查 -> 语法校验 -> 性能优化
     * @param dsl 优化后的DSL对象
     * @return 最终的SQL查询语句
     */
    public String process(DSL dsl) {
        String sql;
        // 优先使用LLM生成SQL，若chatClient未配置则使用规则映射
        if (chatClient != null) {
            sql = generateWithLlm(dsl);
        } else {
            sql = mapper.map(dsl);
        }

        // 安全检查：拦截DROP/DELETE等危险语句
        SQLSecurityChecker.ValidationResult securityResult = securityChecker.check(sql);
        if (!securityResult.isValid()) {
            throw new IllegalArgumentException("Security check failed: " + securityResult.errors());
        }

        // 语法校验：确保SQL以SELECT开头，不含危险关键字
        SQLSyntaxValidator.ValidationResult syntaxResult = syntaxValidator.validate(sql);
        if (!syntaxResult.valid()) {
            throw new IllegalArgumentException("SQL syntax validation failed: " + syntaxResult.errors());
        }

        // 性能优化：去除多余空格
        sql = performanceOptimizer.optimize(sql);
        return sql;
    }

    /**
     * 调用LLM根据DSL生成SQL，失败时回退到规则映射
     */
    private String generateWithLlm(DSL dsl) {
        String dslJson;
        try {
            dslJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dsl);
        } catch (JsonProcessingException e) {
            return mapper.map(dsl);
        }

        // 构建字段映射提示，帮助LLM理解表字段别名
        String fieldMappingPrompt = "";
        if (schema != null) {
            fieldMappingPrompt = schema.getFieldMappingPrompt();
        }

        // 构建LLM提示词，明确指定数据库类型和输出要求
        String promptText = String.format(
                "你是一个SQL生成器。根据以下DSL（Domain Specific Language）定义，生成对应的SQL查询语句。\n\nDSL定义:\n%s\n\n当前数据库类型: %s\n%s\n\n要求:\n1. 只生成SELECT查询语句（禁止INSERT、UPDATE、DELETE等其他任何语句）\n2. 根据DSL中的tables, fields, conditions, joins, group_by, order_by, limit等信息生成完整的SQL\n3. 根据指定的数据库类型(%s)生成正确语法:\n   - PostgreSQL: 使用 DATE_SUB 的正确语法\n   - MySQL: 使用 DATE_SUB(CURDATE(), INTERVAL 25 YEAR)\n   - SQLite: 使用 date('now', '-25 years') 或 date('now', '-7 days')\n4. 确保SQL语法正确\n5. 只返回SQL语句，不要其他内容",
                dslJson, dialect, fieldMappingPrompt.isEmpty() ? "" : "\n" + fieldMappingPrompt, dialect
        );

        try {
            ChatResponse response = chatClient.prompt(new Prompt(promptText)).call().chatResponse();
            String sql = response.getResult().getOutput().getText().trim();
            // 去除可能的Markdown代码块标记
            sql = sql.replaceAll("^```sql\\s*", "").replaceAll("\\s*```$", "").trim();

            // 安全检查：LLM输出必须以SELECT开头
            if (!sql.toUpperCase().startsWith("SELECT")) {
                throw new IllegalArgumentException("LLM generated non-SELECT SQL: " + sql);
            }
            return sql;
        } catch (Exception e) {
            log.warn("LLM SQL generation failed, fallback to mapper: {}", e.getMessage());
            return mapper.map(dsl);
        }
    }
}
