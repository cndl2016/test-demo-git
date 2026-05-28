package com.dm.cn.ragelasticsearch.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 语义感知文本分割器
 * 参考LumberChunker逻辑：先按句子拆分→AI识别语义边界→合并相似句子块
 */
public class SemanticTextSplitter {

    // 句子拆分正则（支持中英文标点）
    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[。！？；.!?;]+");
    // AI提示词模板（核心：识别语义转移的句子ID）
    private static final String SEMANTIC_PROMPT_TEMPLATE = """
            你将接收一组带ID的句子，格式为：ID {序号}: {句子内容}。
            任务：找出第一个与前文内容发生明显语义转移的句子ID（不是第一个句子）。
            要求：
            1. 语义转移指主题、场景、核心信息发生显著变化；
            2. 避免过长的句子块，尽量保持每个块语义独立且长度适中；
            3. 输出格式严格为：Answer: ID {序号}（仅输出这一行）。
            
            句子列表：
            {sentences}
            """;

    /**
     * 核心分割方法
     *
     * @param text             待分割的文本
     * @param maxTokenPerBlock 每个块的最大Token阈值（参考LumberChunker的550词≈660Token）
     * @return 语义分割后的文本块列表
     */
    public List<String> split(ChatModel chatModel, String text, int maxTokenPerBlock) {
        // 步骤1：按句子拆分基础单元
        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return Collections.emptyList();
        }

        // 步骤2：为句子添加ID（便于AI识别边界）
        Map<Integer, String> sentenceWithId = new LinkedHashMap<>();
        for (int i = 0; i < sentences.size(); i++) {
            sentenceWithId.put(i, sentences.get(i));
        }

        // 步骤3：迭代调用AI识别语义边界，合并句子块
        List<Integer> semanticBreakPoints = findSemanticBreakPoints(chatModel, sentenceWithId, maxTokenPerBlock);
        // 步骤4：根据断点拆分最终块
        return mergeByBreakPoints(sentenceWithId, semanticBreakPoints);
    }

    /**
     * 按语法标点拆分句子（支持中英文）
     */
    private List<String> splitIntoSentences(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }
        // 清理空白字符
        String cleanText = text.replaceAll("\\s+", " ").trim();
        return Arrays.stream(SENTENCE_SPLIT_PATTERN.split(cleanText))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 识别语义断点（参考LumberChunker的LLM调用逻辑）
     */
    private List<Integer> findSemanticBreakPoints(ChatModel chatModel, Map<Integer, String> sentenceWithId, int maxTokenPerBlock) {
        List<Integer> breakPoints = new ArrayList<>();
        int currentChunkStart = 0;
        int totalSentences = sentenceWithId.size();

        while (currentChunkStart < totalSentences - 1) {
            // 步骤1：拼接当前候选句子块（不超过Token阈值）
            StringBuilder candidateBlock = new StringBuilder();
            int currentTokenCount = 0;
            int endIndex = currentChunkStart;

            while (endIndex < totalSentences && currentTokenCount < maxTokenPerBlock) {
                String sentence = sentenceWithId.get(endIndex);
                int sentenceTokens = countTokens(sentence);
                if (currentTokenCount + sentenceTokens > maxTokenPerBlock && endIndex > currentChunkStart) {
                    break;
                }
                candidateBlock.append(String.format("ID %d: %s%n", endIndex, sentence));
                currentTokenCount += sentenceTokens;
                endIndex++;
            }

            // 步骤2：调用AI识别语义转移点
            String aiResponse = callAiForSemanticBreakPoint(chatModel, candidateBlock.toString());
            Integer breakPoint = parseBreakPointFromResponse(aiResponse);

            // 步骤3：处理AI响应（无断点则取当前块末尾，有断点则取断点）
            if (breakPoint == null || breakPoint <= currentChunkStart) {
                breakPoint = endIndex - 1;
            }
            breakPoints.add(breakPoint);
            currentChunkStart = breakPoint + 1;
        }

        // 添加最后一个断点（文本末尾）
        breakPoints.add(totalSentences - 1);
        return breakPoints;
    }

    /**
     * 调用AI获取语义断点
     */
    private String callAiForSemanticBreakPoint(ChatModel chatModel, String sentenceBlock) {
        PromptTemplate promptTemplate = new PromptTemplate(SEMANTIC_PROMPT_TEMPLATE);
        Map<String, Object> params = Collections.singletonMap("sentences", sentenceBlock);
        Prompt prompt = promptTemplate.create(params);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    /**
     * 解析AI响应中的断点ID
     */
    private Integer parseBreakPointFromResponse(String aiResponse) {
        if (StringUtils.isBlank(aiResponse)) {
            return null;
        }
        // 匹配 "Answer: ID 123" 格式
        Pattern pattern = Pattern.compile("Answer: ID (\\d+)");
        var matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    /**
     * 根据断点合并句子为最终块
     */
    private List<String> mergeByBreakPoints(Map<Integer, String> sentenceWithId, List<Integer> breakPoints) {
        List<String> finalChunks = new ArrayList<>();
        int lastBreakPoint = -1;

        for (int breakPoint : breakPoints) {
            if (lastBreakPoint == -1) {
                lastBreakPoint = 0;
            }
            // 拼接[lastBreakPoint, breakPoint]区间的句子
            StringBuilder chunk = new StringBuilder();
            for (int i = lastBreakPoint; i <= breakPoint; i++) {
                chunk.append(sentenceWithId.get(i)).append(" ");
            }
            finalChunks.add(chunk.toString().trim());
            lastBreakPoint = breakPoint + 1;
        }

        return finalChunks;
    }

    // 辅助方法：Token计数（参考LumberChunker 1词≈1.2Token）
    private int countTokens(String text) {
        int wordCount = text.split("\\s+").length;
        return (int) Math.round(1.2 * wordCount);
    }
}