package com.dm.cn.model;

import lombok.Data;
import java.util.List;

/** 最终输出：高置信度隐性外键，支持单列与联合 */
@Data
public class FkMatchResult {
    private String fkTable;
    private List<String> fkColumns;
    private String pkTable;
    private List<String> pkColumns;
    private double nameSimilarity;
    private double valueRangeSimilarity;
    private double distributionSimilarity;
    private double sqlConfidence;
    private double totalConfidence;
}
