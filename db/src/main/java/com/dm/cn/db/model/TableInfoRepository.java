package com.dm.cn.db.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TableInfoRepository extends JpaRepository<TableInfo, String> {

    @Query(value = "SELECT t.*, d.description " +
            "FROM information_schema.tables t " +
            "LEFT JOIN pg_catalog.pg_class c ON c.relname = t.table_name " +
            "LEFT JOIN pg_catalog.pg_namespace n ON n.nspname = t.table_schema AND c.relnamespace = n.oid " +
            "LEFT JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 " +
            "WHERE t.table_schema = ?1 AND t.table_type = 'BASE TABLE' ", nativeQuery = true)
    List<TableInfo> getTableInfo(String schemaName);
}