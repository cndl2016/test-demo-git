package com.dm.cn.db.service;

import com.dm.cn.db.model.TableInfo;
import com.dm.cn.db.model.TableInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableInfoService {

    @Autowired
    private TableInfoRepository tableInfoRepository;

    /**
     * 查询指定schema下指定所有表的信息
     * @param schemaName 模式名
     * @return 表信息列表
     */
    public List<TableInfo> getTableInfo(String schemaName) {
        return tableInfoRepository.getTableInfo(schemaName);
    }
}