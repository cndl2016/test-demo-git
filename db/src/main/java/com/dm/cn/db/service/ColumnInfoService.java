package com.dm.cn.db.service;

import com.dm.cn.db.model.ColumnInfo;
import com.dm.cn.db.model.ColumnInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ColumnInfoService {

    @Autowired
    private ColumnInfoRepository columnInfoRepository;

    /**
     * 查询指定schema下指定表的所有字段信息
     * @param schemaName 模式名
     * @param tableName 表名
     * @return 字段信息列表
     */
    public List<ColumnInfo> getColumns(String schemaName, String tableName) {
        return columnInfoRepository.getColumns(schemaName, tableName);
    }
}