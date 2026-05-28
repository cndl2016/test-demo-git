package com.dm.cn.ragmilvus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class MilvusVo implements Serializable {
    /**
     * 主键ID（自定义UUID，非自增）
     * 对应Milvus字段：id (VarChar, 64)
     */
    @JsonProperty("id")
    private String id;

    /**
     * 向量字段（文本嵌入后的浮点向量）
     * 对应Milvus字段：embedding (FloatVector, 维度1024)
     */
    @JsonProperty("embedding")
    private float[] embedding;

    /**
     * 文档ID（关联业务文档的唯一标识）
     * 对应Milvus字段：doc_id (VarChar, 64)
     */
    @JsonProperty("doc_id")
    private String docId;

    /**
     * 文档所属用户ID
     * 对应Milvus字段：owner_id (Int64)
     */
    @JsonProperty("owner_id")
    private Long ownerId;

    /**
     * 可见性标识（1：可见，0：不可见）
     * 对应Milvus字段：visibility (Int8)
     */
    @JsonProperty("visibility")
    private Integer visibility;

    /**
     * 分组ID集合（CSV格式，如：group1,group2）
     * 对应Milvus字段：group_ids_csv (VarChar, 512)
     */
    @JsonProperty("group_ids_csv")
    private String groupIdsCsv;

    /**
     * 数据来源（如：manual_input、dynamic_generation）
     * 对应Milvus字段：source (VarChar, 256)
     */
    @JsonProperty("source")
    private String source;

    /**
     * 分片索引（同文档下的分片序号）
     * 对应Milvus字段：chunk_index (Int32)
     */
    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    /**
     * 文本分片内容
     * 对应Milvus字段：chunk (VarChar, 16384)
     */
    @JsonProperty("chunk")
    private String chunk;

    /**
     * 相似度得分（检索时新增字段，非Milvus原始字段）
     */
    private Float similarityScore;
}