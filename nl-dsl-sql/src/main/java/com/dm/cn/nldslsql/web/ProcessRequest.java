package com.dm.cn.nldslsql.web;

import lombok.Data;

/**
 * 查询处理请求对象
 * 接收前端传入的自然语言查询文本
 */
@Data
public class ProcessRequest {
    /** 用户输入的自然语言查询 */
    private String query;
}
