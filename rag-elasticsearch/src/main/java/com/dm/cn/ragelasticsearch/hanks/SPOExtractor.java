package com.dm.cn.ragelasticsearch.hanks;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * HanLP 1.x 基于PKU标注体系的SPO三元组抽取Demo
 * 主语(S)：n/nr/ns/nt（普通名词/人名/地名/机构名）
 * 谓语(P)：v（普通动词）
 * 宾语(O)：n/vn（普通名词/名动词）
 */
public class SPOExtractor {

    // 定义SPO三元组实体类
    static class SPO {
        private String subject;    // 主语
        private String predicate;  // 谓语
        private String object;     // 宾语

        public SPO(String subject, String predicate, String object) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
        }

        @Override
        public String toString() {
            return "SPO三元组：\n" +
                    "  主语(S)：" + subject + "\n" +
                    "  谓语(P)：" + predicate + "\n" +
                    "  宾语(O)：" + object;
        }
    }

    /**
     * 核心方法：抽取SPO三元组
     *
     * @param text 待抽取的文本
     * @return 筛选后的SPO三元组列表
     */
    public static List<SPO> extractSPO(String text) {
        List<SPO> spoList = new ArrayList<>();

        // 前置校验
        if (text == null || text.trim().isEmpty()) {
            System.out.println("待抽取文本为空");
            return spoList;
        }

        // 执行依存句法分析
        CoNLLSentence sentence = HanLP.parseDependency(text);
        if (sentence == null) {
            System.out.println("句法分析结果为空");
            return spoList;
        }

        // 找到核心谓语（依存关系为“核心关系”且词性为v的词）
        CoNLLWord corePredicate = null;
        for (CoNLLWord word : sentence) {
            // 核心谓语特征：依存关系=核心关系 + 词性=v（动词）
            if ("核心关系".equals(word.DEPREL) && isVerbPos(word.POSTAG)) {
                corePredicate = word;
                System.out.println("找到核心谓语：" + corePredicate.LEMMA + "（词性：" + corePredicate.POSTAG + "）");
                break;
            }
        }

        // 找主语（依存关系为“主谓关系”且指向核心谓语的词）
        String subject = "";
        for (CoNLLWord word : sentence) {
            // 主语特征：头词是核心谓语 + 依存关系=主谓关系 + 词性符合主语规则
            if (word.HEAD != null && word.HEAD == corePredicate
                    && "主谓关系".equals(word.DEPREL)
                    && isSubjectPos(word.POSTAG)) {
                subject = word.LEMMA;
                System.out.println("找到主语：" + subject + "（词性：" + word.POSTAG + "）");
                break;
            }
        }

        // 找宾语（依存关系为“动宾关系”且指向核心谓语的词，含定中关系的修饰词）
        String object = "";
        for (CoNLLWord word : sentence) {
            // 宾语核心特征：头词是核心谓语 + 依存关系=动宾关系 + 词性符合宾语规则
            if (word.HEAD != null && word.HEAD == corePredicate
                    && "动宾关系".equals(word.DEPREL)
                    && isObjectPos(word.POSTAG)) {
                // 检查是否有定中关系的修饰词（如“新款”修饰“产品”）
                String modifier = getModifier(word, sentence);
                object = modifier + (modifier.isEmpty() ? "" : "") + word.LEMMA;
                System.out.println("找到宾语：" + object + "（核心词：" + word.LEMMA + "，修饰词：" + modifier + "）");
                break;
            }
        }

        // 封装SPO三元组（允许宾语为空）
        if (!subject.isEmpty() && !corePredicate.LEMMA.isEmpty()) {
            spoList.add(new SPO(subject, corePredicate.LEMMA, object));
        }

        return spoList;
    }


    /**
     * 获取宾语的修饰词
     *
     * @param objectCore 宾语核心词
     * @param sentence   完整句法分析结果
     * @return 修饰词
     */
    private static String getModifier(CoNLLWord objectCore, CoNLLSentence sentence) {
        for (CoNLLWord word : sentence) {
            // 修饰词特征：头词是宾语核心词 + 依存关系=定中关系
            if (word.HEAD != null && word.HEAD == objectCore && "定中关系".equals(word.DEPREL)) {
                return word.LEMMA;
            }
        }
        return "";
    }

    /**
     * 判断是否为动词词性（适配HanLP返回的v/vn等）
     */
    private static boolean isVerbPos(String pos) {
        return pos.startsWith("v");
    }

    /**
     * 判断是否为合法的主语词性（PKU标注：n/nr/ns/nt）
     *
     * @param pos 词性标注
     * @return 符合返回true，否则false
     */
    private static boolean isSubjectPos(String pos) {
        return pos.startsWith("n");
    }

    /**
     * 判断是否为合法的宾语词性（PKU标注：n/vn）
     *
     * @param pos 词性标注
     * @return 符合返回true，否则false
     */
    private static boolean isObjectPos(String pos) {
        return "n".equals(pos) || "vn".equals(pos);
    }

    /**
     * 辅助方法：打印分词+PKU词性标注结果（便于调试）
     *
     * @param text 待分析文本
     */
    public static void printWordWithPKUPos(String text) {
        List<Term> termList = HanLP.segment(text);
        System.out.println("分词+PKU词性标注结果：");
        for (Term term : termList) {
            System.out.println(term.word + ":" + term.nature);
        }
        System.out.println("------------------------");
    }

    // 主方法测试
    public static void main(String[] args) {

        // 测试文本（覆盖不同场景：人名、机构名、普通名词作主语）
        String[] testTexts = {
                "母亲在九岁时便早早地奔赴沪上做童工谋生，在这座城市，她遇到了冯裕才的父亲",
                "去中心化使加密货币用户能更好地控制其资金",
                "我去超市买东西",
                "北京大学成立于1898年",
                "马云在阿里巴巴发布了新款产品",
                "北京举办了2008年奥运会",
                "苹果公司推出了iPhone 16",
                "I’m going to the supermarket to buy some things",
        };

        // 遍历测试文本，抽取SPO
        for (String text : testTexts) {
            System.out.println("======== 待分析文本：" + text + " ========");
            // 打印分词+PKU标注（调试用）
            printWordWithPKUPos(text);
            // 抽取SPO
            List<SPO> spoList = extractSPO(text);
            // 输出结果
            if (spoList.isEmpty()) {
                System.out.println("未抽取到符合PKU标注的SPO三元组");
            } else {
                for (SPO spo : spoList) {
                    System.out.println(spo);
                }
            }
            System.out.println("\n");
        }
    }
}