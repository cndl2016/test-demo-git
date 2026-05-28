package com.dm.cn.db.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tables", schema = "information_schema")
public class TableInfo {

    @EmbeddedId
    private TableId id;

    private String table_type;
    private String self_referencing_column_name;
    private String reference_generation;
    private String user_defined_type_catalog;
    private String user_defined_type_schema;
    private String user_defined_type_name;
    private String is_insertable_into;
    private String is_typed;
    private String commit_action;
}
