package com.dm.cn.ragelasticsearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSplitterConfig {
    // 文件路径
    private String filePath;

    // 分割类型
    private String splitterType;

    // 每个分块的最大字符长度
    private Integer chunkSize;

    // 是否保留分隔符
    private Boolean keepSeparator;

    // 自定义分隔符列表
    private List<String> delimiters;

    // 是否替换连续的空格、换行符和制表符
    private Boolean ignoreWhitespace;

    // 分块重叠度百分比
    private Integer overlap;
}
