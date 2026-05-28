package com.dm.cn.db.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TableConstraintRepository extends JpaRepository<TableConstraint, String> {
    @Query(value = "SELECT t.constraint_catalog, " +
            "t.constraint_schema, " +
            "t.constraint_name, " +
            "t.table_catalog, " +
            "t.table_schema, " +
            "t.table_name, " +
            "t.constraint_type, " +
            "t.is_deferrable, " +
            "t.initially_deferred, " +
            "t.enforced, " +
            "c.table_name AS primary_table, " +
            "c.column_name AS primary_column, " +
            "t.table_name AS foreign_table, " +
            "k.column_name AS foreign_column, " +
            "k.ordinal_position, " +
            "k.position_in_unique_constraint " +
            "FROM information_schema.table_constraints t " +
            "LEFT JOIN information_schema.constraint_column_usage c  " +
            "ON t.constraint_name = c.constraint_name " +
            "LEFT JOIN information_schema.key_column_usage k  " +
            "ON t.constraint_name = k.constraint_name " +
            "WHERE t.table_schema = ?1 AND t.table_name = ?2 ",
            nativeQuery = true)
    List<TableConstraint> getConstraints(String schemaName, String tableName);
}