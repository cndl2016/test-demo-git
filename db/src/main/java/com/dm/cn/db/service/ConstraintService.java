package com.dm.cn.db.service;

import com.dm.cn.db.model.TableConstraint;
import com.dm.cn.db.model.TableConstraintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConstraintService {

    @Autowired
    private TableConstraintRepository constraintRepo;

    /**
     * 查询指定schema下指定表的所有约束信息
     * @param schemaName 模式名
     * @param tableName 表名
     * @return 约束信息列表
     */
    public List<TableConstraint> getConstraints(String schemaName, String tableName) {
        return constraintRepo.getConstraints(schemaName, tableName);
    }
}