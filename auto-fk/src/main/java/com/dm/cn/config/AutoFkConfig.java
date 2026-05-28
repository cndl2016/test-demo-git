package com.dm.cn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Auto-FK 核心配置：阈值、线程池、白名单
 */
@Configuration
public class AutoFkConfig {

    // ====================== 防误杀核心阈值（宽松初筛） ======================
    // 步骤2：值域重合度阈值（低阈值=0.1，避免误杀）
    public static final double VALUE_RANGE_THRESHOLD = 0.1;
    // 步骤3：命名相似度阈值（低阈值=0.3，支持缩写/语义相近）
    public static final double NAME_SIMILARITY_THRESHOLD = 0.3;
    // 步骤4：分布相似度阈值（高精度校验=0.6）
    public static final double DISTRIBUTION_SIMILARITY_THRESHOLD = 0.6;
    // 最终综合置信度阈值
    public static final double TOTAL_CONFIDENCE_THRESHOLD = 0.7;

    // 权重配置
    public static final double WEIGHT_NAME = 0.3;
    public static final double WEIGHT_VALUE = 0.2;
    public static final double WEIGHT_DISTRIBUTION = 0.4;
    public static final double WEIGHT_SQL = 0.1;

    // 并行计算线程池
    @Bean("autoFkExecutor")
    public ExecutorService autoFkExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}