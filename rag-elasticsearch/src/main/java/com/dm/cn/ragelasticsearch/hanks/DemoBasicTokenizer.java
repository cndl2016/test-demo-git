/*
 * <summary></summary>
 * <author>hankcs</author>
 * <email>me@hankcs.com</email>
 * <create-date>2015/5/4 23:38</create-date>
 *
 * <copyright file="DemoBasicTokenizer.java">
 * Copyright (c) 2003-2015, hankcs. All Right Reserved, http://www.hankcs.com/
 * </copyright>
 */
package com.dm.cn.ragelasticsearch.hanks;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.BasicTokenizer;

import java.util.List;

/**
 * 演示基础分词，基础分词只进行基本NGram分词，不识别命名实体，不使用用户词典
 *
 * @author hankcs
 */
public class DemoBasicTokenizer
{
    public static void main(String[] args)
    {
        String text = "小明在公园踢足球，他玩得很开心。";
        System.out.println(BasicTokenizer.segment(text));
        // 测试分词速度，让大家对HanLP的性能有一个直观的认识
        long start = System.currentTimeMillis();
        int pressure = 100000;
        for (int i = 0; i < pressure; ++i)
        {
            BasicTokenizer.segment(text);
        }
        double costTime = (System.currentTimeMillis() - start) / (double) 1000;
        System.out.printf("BasicTokenizer分词速度：%.2f字每秒\n", text.length() * pressure / costTime);
    }
}
