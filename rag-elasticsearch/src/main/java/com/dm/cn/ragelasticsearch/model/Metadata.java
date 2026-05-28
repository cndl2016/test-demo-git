package com.dm.cn.ragelasticsearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Metadata {

    private String location;

    @JsonProperty("ref_doc_id")
    private String refDocId;

    private String source;
}
