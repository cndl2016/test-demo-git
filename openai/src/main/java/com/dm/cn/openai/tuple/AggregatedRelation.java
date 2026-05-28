package com.dm.cn.openai.tuple;

import java.util.List;

/**
 * 聚合后的最终关系（一对概念仅一条）
 */
public class AggregatedRelation {
    private String node1;
    private String node2;
    private double totalWeight; // W1+W2总和
    private List<String> edges; // 拼接的所有关系描述

    // 构造器 & Getter & Setter
    public AggregatedRelation() {}

    public AggregatedRelation(String node1, String node2, double totalWeight, List<String> edges) {
        this.node1 = node1;
        this.node2 = node2;
        this.totalWeight = totalWeight;
        this.edges = edges;
    }

    // Getter & Setter 略
    public String getNode1() { return node1; }
    public void setNode1(String node1) { this.node1 = node1; }
    public String getNode2() { return node2; }
    public void setNode2(String node2) { this.node2 = node2; }
    public double getTotalWeight() { return totalWeight; }
    public void setTotalWeight(double totalWeight) { this.totalWeight = totalWeight; }
    public List<String> getEdges() { return edges; }
    public void setEdges(List<String> edges) { this.edges = edges; }
}
