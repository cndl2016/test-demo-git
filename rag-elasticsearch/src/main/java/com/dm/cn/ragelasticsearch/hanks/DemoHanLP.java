package com.dm.cn.ragelasticsearch.hanks;

public class DemoHanLP {
    public static void main(String[] args) {
        String content = "这一切的起因是：是黑人乔治·佛洛伊德（George Floyd）之死。\n" +
                "46岁的明尼阿波利斯黑人男子弗洛伊德，被怀疑使用假钞。\n" +
                "一名白人警察在执法过程中，将自己膝盖压在了弗洛伊德的脖子上。几分钟后男子窒息死亡。\n" +
                "白人警察当街杀死了黑人平民，美国人愤怒了。\n" +
                "从当地时间29日晚起，美国当地抗议者走上了全美22州及华盛顿特区共33个城市街头进行抗议示威。\n" +
                "其中一些抗议游行转变为了暴力游行。\n" +
                "警车被点燃。";
        TextMine textMine = new TextMine();
        textMine.buildGraph(content);
    }
}
