package com.dm.cn.model;

import lombok.Data;
import java.util.List;

/** 候选外键源：支持单列与联合外键 */
@Data
public class ForeignKeySourceCandidate {
    private String tableName;
    private List<String> columnNames;   // 外键列列表，单列时size=1
    private List<String> dataTypes;     // 对应数据类型列表
    private boolean nullable;           // 至少一列可为空
}
