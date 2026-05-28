package com.dm.cn.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 数据库元数据查询（基于 Druid + JdbcTemplate）
 */
@Component
public class DbMetaMapper {

    @Resource
    private JdbcTemplate jdbcTemplate;

    // ====================== 1. 查询所有显式主键（PG版，包含联合主键） ======================
    public List<Map<String, Object>> selectExplicitPrimaryKeys() {
        String sql = "SELECT " +
                "    tc.table_name AS table_name, " +
                "    tc.constraint_name AS constraint_name, " +
                "    kcu.column_name AS column_name, " +
                "    c.data_type AS data_type, " +
                "    kcu.ordinal_position AS ordinal_position " +
                "FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu " +
                "    ON tc.constraint_name = kcu.constraint_name " +
                "    AND tc.table_schema = kcu.table_schema " +
                "JOIN information_schema.columns c " +
                "    ON c.table_name = tc.table_name " +
                "    AND c.column_name = kcu.column_name " +
                "    AND c.table_schema = tc.table_schema " +
                "WHERE " +
                "    tc.constraint_type = 'PRIMARY KEY' " +
                "    AND tc.table_schema = current_schema() " +
                "ORDER BY tc.constraint_name, kcu.ordinal_position";
        return jdbcTemplate.queryForList(sql);
    }

    // ====================== 2. 查询当前Schema所有表的所有列（PG版） ======================
    public List<Map<String, Object>> selectAllColumns() {
        String sql = "SELECT " +
                "    table_name AS table_name, " +
                "    column_name AS column_name, " +
                "    data_type AS data_type, " +
                "    is_nullable AS is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_schema = current_schema() " +
                "ORDER BY table_name, ordinal_position";
        return jdbcTemplate.queryForList(sql);
    }

    // ====================== 3. 计算列唯一值比例（PG通用） ======================
    public double selectColumnUniqueRatio(String table, String column) {
        String safeTable = quoteIdentifier(table);
        String safeColumn = quoteIdentifier(column);
        String sql = String.format(
                "SELECT COUNT(DISTINCT %s)::FLOAT / COUNT(*) AS unique_ratio FROM %s",
                safeColumn, safeTable);
        Double result = jdbcTemplate.queryForObject(sql, Double.class);
        return result != null ? result : 0.0;
    }

    // ====================== 4. 计算两个字段值域交集（PG版，单列） ======================
    public long selectIntersectionCount(String fkTable, String fkCol, String pkTable, String pkCol) {
        String safeFkTable = quoteIdentifier(fkTable);
        String safeFkCol = quoteIdentifier(fkCol);
        String safePkTable = quoteIdentifier(pkTable);
        String safePkCol = quoteIdentifier(pkCol);
        String sql = String.format(
                "SELECT COUNT(DISTINCT t1.%s) AS intersection " +
                        "FROM %s t1 " +
                        "JOIN %s t2 ON t1.%s = t2.%s",
                safeFkCol, safeFkTable, safePkTable, safeFkCol, safePkCol);
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // ====================== 4.1 计算多列组合值域交集（联合键） ======================
    public long selectIntersectionCountComposite(String fkTable, List<String> fkCols, String pkTable, List<String> pkCols) {
        String safeFkTable = quoteIdentifier(fkTable);
        String safePkTable = quoteIdentifier(pkTable);

        StringBuilder joinCondition = new StringBuilder();
        for (int i = 0; i < fkCols.size(); i++) {
            if (i > 0) joinCondition.append(" AND ");
            joinCondition.append(String.format("t1.%s = t2.%s",
                    quoteIdentifier(fkCols.get(i)), quoteIdentifier(pkCols.get(i))));
        }

        StringBuilder distinctCols = new StringBuilder();
        for (int i = 0; i < fkCols.size(); i++) {
            if (i > 0) distinctCols.append(", ");
            distinctCols.append(quoteIdentifier(fkCols.get(i)));
        }

        String sql = String.format(
                "SELECT COUNT(DISTINCT (%s)) AS intersection FROM %s t1 JOIN %s t2 ON %s",
                distinctCols, safeFkTable, safePkTable, joinCondition);
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // ====================== 5. 字段去重计数（单列） ======================
    public long selectDistinctCount(String table, String col) {
        String safeTable = quoteIdentifier(table);
        String safeCol = quoteIdentifier(col);
        String sql = String.format("SELECT COUNT(DISTINCT %s) FROM %s", safeCol, safeTable);
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // ====================== 5.1 字段组合去重计数（联合键） ======================
    public long selectDistinctCountComposite(String table, List<String> cols) {
        String safeTable = quoteIdentifier(table);
        StringBuilder distinctCols = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) distinctCols.append(", ");
            distinctCols.append(quoteIdentifier(cols.get(i)));
        }
        String sql = String.format("SELECT COUNT(DISTINCT (%s)) FROM %s", distinctCols, safeTable);
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    // 获取字段所有值（单列）
    public List<Object> selectColumnValues(String table, String column) {
        String safeTable = quoteIdentifier(table);
        String safeColumn = quoteIdentifier(column);
        String sql = String.format("SELECT %s AS val FROM %s LIMIT 10000", safeColumn, safeTable);
        return jdbcTemplate.queryForList(sql, Object.class);
    }

    // 获取多列组合值（联合键）
    public List<Map<String, Object>> selectCompositeColumnValues(String table, List<String> columns) {
        String safeTable = quoteIdentifier(table);
        StringBuilder colStr = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) colStr.append(", ");
            colStr.append(quoteIdentifier(columns.get(i)));
        }
        String sql = String.format("SELECT %s FROM %s LIMIT 10000", colStr, safeTable);
        return jdbcTemplate.queryForList(sql);
    }

    // 获取分类字段值
    public List<Object> selectCategoryValues(String table, String column) {
        return selectColumnValues(table, column);
    }

    // ====================== 工具方法：PG标识符转义，防SQL注入 ======================
    private String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be null or empty");
        }
        if (!identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
