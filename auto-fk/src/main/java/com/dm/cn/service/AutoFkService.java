package com.dm.cn.service;

import com.dm.cn.model.FkMatchResult;

import java.util.List;

/**
 * Auto-FK 隐性外键识别 核心服务接口
 */
public interface AutoFkService {

    /**
     * 执行 Auto-FK 全流程：识别隐性外键关系
     * @return 高置信度外键匹配结果
     */
    List<FkMatchResult> executeAutoFk();
}