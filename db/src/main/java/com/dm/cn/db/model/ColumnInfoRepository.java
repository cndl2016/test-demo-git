package com.dm.cn.db.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ColumnInfoRepository extends JpaRepository<ColumnInfo, ColumnId> {

    // 根据schema和table查询所有字段
    @Query(value = "SELECT col.*,d.description " +
            "FROM pg_catalog.pg_description d " +
            "JOIN pg_catalog.pg_class c ON d.objoid = c.oid " +
            "JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid " +
            "JOIN pg_catalog.pg_attribute a ON d.objoid = a.attrelid AND d.objsubid = a.attnum " +
            "JOIN information_schema.columns col ON col.table_schema = n.nspname AND col.table_name = c.relname AND col.column_name = a.attname " +
            "WHERE n.nspname = ?1 " +
            "AND c.relname = ?2 " +
            "AND a.attnum > 0 " +
            "ORDER BY col.ordinal_position ", nativeQuery = true)
    List<ColumnInfo> getColumns(String schemaName, String tableName);
}