package com.dm.cn.db.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

// 复合主键类
@Data
@Embeddable
public class TableId implements Serializable {
    private String table_catalog;
    private String table_schema;
    private String table_name;

    // 必须实现equals和hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableId tableId = (TableId) o;
        return Objects.equals(table_catalog, tableId.table_catalog) &&
                Objects.equals(table_schema, tableId.table_schema) &&
                Objects.equals(table_name, tableId.table_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table_catalog, table_schema, table_name);
    }
}