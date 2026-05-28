package com.dm.cn.nldslsql.web;

import com.dm.cn.nldslsql.core.NL2SQLEngine;
import com.dm.cn.nldslsql.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NL2DSL2SQL REST API控制器
 * 提供自然语言查询处理的HTTP接口
 */
@Slf4j
@RestController
@RequestMapping("/nl2dslsql")
@RequiredArgsConstructor
public class Nl2Dsl2SqlController {

    /** NL2SQL引擎核心服务 */
    private final NL2SQLEngine engine;

    /**
     * 处理自然语言查询请求
     * @param request 包含自然语言查询的请求对象
     * @return 查询处理结果，包含生成的SQL和DSL
     */
    @PostMapping("/process")
    public QueryResult process(@RequestBody ProcessRequest request) {
        log.info("Received query: {}", request.getQuery());
        return engine.process(request.getQuery());
    }
}
