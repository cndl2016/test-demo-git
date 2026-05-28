package com.dm.cn.nldslsql.modules.sqlgen;

import java.util.regex.Pattern;

/**
 * SQL性能优化器
 * 对生成的SQL进行轻量级格式化优化：压缩多余空格、去除首尾空白
 * 提升SQL可读性，同时减少传输体积
 */
public class SQLPerformanceOptimizer {
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    /**
     * 优化SQL语句格式
     * @param sql 原始SQL
     * @return 优化后的SQL
     */
    public String optimize(String sql) {
        // 将多个连续空白字符压缩为单个空格
        sql = MULTIPLE_SPACES.matcher(sql).replaceAll(" ");
        return sql.trim();
    }
}
