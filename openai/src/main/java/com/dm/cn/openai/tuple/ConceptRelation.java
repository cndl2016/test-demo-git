package com.dm.cn.openai.tuple;

/**
 * 概念对关系实体（包含权重和关系描述）
 */
public class ConceptRelation {
    // 归一化后的概念对（避免A-B和B-A重复）
    private String node1;
    private String node2;
    // 关系描述（语义关系/上下文关系）
    private String edge;
    // 权重
    private double weightW1;
    private double weightW2;
    // 所属文本块ID
    private String chunkId;

    // 构造器 & Getter & Setter
    public ConceptRelation() {}

    public ConceptRelation(String node1, String node2, String edge, double weightW1, double weightW2, String chunkId) {
        // 归一化概念对顺序（按字符串排序）
        this.node1 = node1.compareTo(node2) < 0 ? node1 : node2;
        this.node2 = node1.compareTo(node2) < 0 ? node2 : node1;
        this.edge = edge;
        this.weightW1 = weightW1;
        this.weightW2 = weightW2;
        this.chunkId = chunkId;
    }

    // Getter & Setter 略（可通过IDE自动生成）
    public String getNode1() { return node1; }
    public void setNode1(String node1) { this.node1 = node1; }
    public String getNode2() { return node2; }
    public void setNode2(String node2) { this.node2 = node2; }
    public String getEdge() { return edge; }
    public void setEdge(String edge) { this.edge = edge; }
    public double getWeightW1() { return weightW1; }
    public void setWeightW1(double weightW1) { this.weightW1 = weightW1; }
    public double getWeightW2() { return weightW2; }
    public void setWeightW2(double weightW2) { this.weightW2 = weightW2; }
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
}