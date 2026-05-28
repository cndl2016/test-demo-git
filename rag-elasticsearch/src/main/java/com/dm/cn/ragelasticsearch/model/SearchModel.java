package com.dm.cn.ragelasticsearch.model;

import lombok.Data;

@Data
public class SearchModel {

    private String source;

    private String location;

    private String refDocId;

    private Integer pageIndex;

    private Integer pageSize;
}
