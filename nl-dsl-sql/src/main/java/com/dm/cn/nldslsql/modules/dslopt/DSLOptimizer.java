package com.dm.cn.nldslsql.modules.dslopt;

import com.dm.cn.nldslsql.config.SecurityProperties;
import com.dm.cn.nldslsql.model.DSL;

import java.util.HashMap;
import java.util.Map;

/**
 * DSL优化器
 * 对DSL进行安全验证、标准化和性能优化
 * 是整个NL2SQL流水线的第四阶段
 */
public class DSLOptimizer {
    private final SecurityValidator securityValidator;
    private final DSLStandardizer standardizer;
    private final PerformanceOptimizer performanceOptimizer;

    public DSLOptimizer(SecurityProperties config) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("allowed_operations", config.getAllowedOperations());
        configMap.put("max_query_depth", config.getMaxQueryDepth());
        this.securityValidator = new SecurityValidator(configMap);
        this.standardizer = new DSLStandardizer();
        this.performanceOptimizer = new PerformanceOptimizer();
    }

    /**
     * 处理DSL优化流程：安全验证 -> 标准化 -> 性能优化
     * @param dsl 待优化的DSL
     * @return 优化后的DSL
     */
    public DSL process(DSL dsl) {
        // 安全验证
        SecurityValidator.ValidationResult validation = securityValidator.validate(dsl);
        if (!validation.valid()) {
            throw new IllegalArgumentException("Security validation failed: " + validation.errors());
        }

        // 标准化处理
        dsl = standardizer.standardize(dsl);
        // 性能优化
        dsl = performanceOptimizer.optimize(dsl);
        return dsl;
    }

    /**
     * 生成优化报告，对比原始DSL和优化后的DSL
     */
    public Map<String, Object> getOptimizationReport(DSL originalDsl, DSL optimizedDsl) {
        Map<String, Object> changes = new HashMap<>();
        changes.put("fields_optimized", !originalDsl.getFields().equals(optimizedDsl.getFields()));
        changes.put("joins_optimized", originalDsl.getJoins().size() != optimizedDsl.getJoins().size());
        changes.put("conditions_optimized", originalDsl.getConditions().size() != optimizedDsl.getConditions().size());

        Map<String, Object> report = new HashMap<>();
        report.put("original", originalDsl);
        report.put("optimized", optimizedDsl);
        report.put("changes", changes);
        return report;
    }
}
