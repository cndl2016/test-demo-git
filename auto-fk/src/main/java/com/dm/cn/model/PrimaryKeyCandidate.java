package com.dm.cn.model;

import lombok.Data;
import java.util.List;

/** 候选主键：支持单列与联合主键 */
@Data
public class PrimaryKeyCandidate {
    private String tableName;
    private List<String> columnNames;   // 主键列列表，单列时size=1
    private List<String> dataTypes;     // 对应数据类型列表
    private boolean isExplicitPk;       // 是否显式主键
}
