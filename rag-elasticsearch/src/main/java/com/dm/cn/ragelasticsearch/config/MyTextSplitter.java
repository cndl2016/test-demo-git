package com.dm.cn.ragelasticsearch.config;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化的文本分割器，按指定分隔符和字符长度限制分割文本
 */
public class MyTextSplitter extends TextSplitter {
    // 每个分块的最大字符长度(100-5000)
    private final int chunkSize;
    // 是否保留分隔符
    private final boolean keepSeparator;
    // 自定义分隔符列表
    private final List<String> delimiters;
    // 是否替换连续的空格、换行符和制表符
    private final boolean ignoreWhitespace;
    // 分块重叠度百分比(0-90)
    private final int overlap;

    /**
     * 全参数构造函数
     * @param chunkSize 每个分块的最大字符长度(100-5000)
     * @param keepSeparator 是否保留分隔符
     * @param delimiters 分隔符列表
     * @param ignoreWhitespace 是否替换连续的空格、换行符和制表符
     * @param overlap 分块重叠度百分比(0-90)
     */
    public MyTextSplitter(int chunkSize, boolean keepSeparator, List<String> delimiters, boolean ignoreWhitespace, int overlap) {
        if (chunkSize < 100 || chunkSize > 5000) {
            throw new IllegalArgumentException("每个块大小必须在100-5000之间");
        }
        if (overlap < 0 || overlap > 90) {
            throw new IllegalArgumentException("重叠度必须在0-90之间");
        }
        this.chunkSize = chunkSize;
        this.keepSeparator = keepSeparator;
        this.delimiters = delimiters;
        this.ignoreWhitespace = ignoreWhitespace;
        this.overlap = overlap;
    }

    /**
     * 重写父类方法，实现文本分割
     * @param text 要分割的文本
     * @return 分割后的文本块列表
     */
    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 如果需要忽略连续空白字符，先进行替换处理
        text = ignoreWhitespace ? text.replaceAll("\\s+", " ") : text;
        // 计算实际重叠字符数
        int overlapChars = (int) (chunkSize * overlap / 100.0);

        List<String> chunks = new ArrayList<>();
        int currentPosition = 0;
        int textLength = text.length();

        while (currentPosition < textLength) {
            // 计算当前允许的最大结束位置（不超过文本长度）
            int maxEndPosition = Math.min(currentPosition + chunkSize, textLength);

            // 查找当前位置后最近的分隔符
            int nextSplitPosition = findNextSplitPosition(text, currentPosition, maxEndPosition);

            // 确定实际分割位置
            int splitPosition;
            if (nextSplitPosition != -1) {
                // 找到有效分隔符，分割在分隔符后
                splitPosition = nextSplitPosition + 1;
            } else {
                // 未找到有效分隔符，按最大长度分割
                splitPosition = maxEndPosition;
            }

            // 根据分块重叠度计算开始位置
            int startPosition = currentPosition - overlapChars < 0 ? 0 : currentPosition - overlapChars;
            // 提取分块文本
            String chunkText = text.substring(startPosition, splitPosition);
            String processedChunk = processChunkText(chunkText);

            if (!processedChunk.isEmpty()) {
                chunks.add(processedChunk);
            }

            currentPosition = splitPosition;
        }

        return chunks;
    }

    /**
     * 查找当前位置到最大结束位置之间最近的分隔符
     */
    private int findNextSplitPosition(String text, int startPosition, int maxEndPosition) {
        int minPosition = -1;

        for (String delimiter : delimiters) {
            int position = text.indexOf(delimiter, startPosition);
            // 只考虑在[startPosition, maxEndPosition)范围内的分隔符
            if (position != -1 && position < maxEndPosition) {
                if (minPosition == -1 || position < minPosition) {
                    minPosition = position;
                }
            }
        }

        return minPosition;
    }

    /**
     * 处理分块文本
     */
    private String processChunkText(String chunkText) {
        chunkText = chunkText.trim();
        // 根据配置决定是否保留分隔符
        if (!keepSeparator && !chunkText.isEmpty()) {
            for (String delimiter : delimiters) {
                if (chunkText.endsWith(delimiter)) {
                    return chunkText.substring(0, chunkText.length() - delimiter.length()).trim();
                }
            }
        }
        return chunkText;
    }
}