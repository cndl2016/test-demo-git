package com.dm.cn.ragelasticsearch.model;

import lombok.Data;

@Data
public class DocumentChunk {
    private String id;
    private String content;
    private float[] embedding;
    private Metadata metadata;
}