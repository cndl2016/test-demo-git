package com.dm.cn.db.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "table_constraints", schema = "information_schema")
public class TableConstraint {

    private String constraint_catalog;
    private String constraint_schema;
    @Id
    private String constraint_name;
    private String table_catalog;
    private String table_schema;
    private String table_name;
    private String constraint_type;
    private String is_deferrable;
    private String initially_deferred;
    private String enforced;

    private String primary_table;
    private String primary_column;

    private String foreign_table;
    private String foreign_column;
    private Integer ordinal_position;
    private Integer position_in_unique_constraint;
}