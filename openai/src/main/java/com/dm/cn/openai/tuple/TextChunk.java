package com.dm.cn.openai.tuple;

import java.util.UUID;

/**
 * 文本分块实体，包含chunk_id和文本内容
 */
public class TextChunk {
    private String chunkId;
    private String content;

    public TextChunk(String content) {
        this.chunkId = UUID.randomUUID().toString().replace("-", "");
        this.content = content;
    }

    // Getter & Setter
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
