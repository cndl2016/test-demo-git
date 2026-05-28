package com.dm.cn.db.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.Objects;

@Data
@Embeddable
public class ColumnId implements Serializable {
    private String table_catalog;
    private String table_schema;
    private String table_name;
    private String column_name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnId columnId = (ColumnId) o;
        return Objects.equals(table_catalog, columnId.table_catalog) &&
                Objects.equals(table_schema, columnId.table_schema) &&
                Objects.equals(table_name, columnId.table_name) &&
                Objects.equals(column_name, columnId.column_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table_catalog, table_schema, table_name, column_name);
    }
}