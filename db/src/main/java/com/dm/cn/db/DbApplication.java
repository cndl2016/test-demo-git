package com.dm.cn.db;

import com.dm.cn.db.model.ColumnInfo;
import com.dm.cn.db.model.TableConstraint;
import com.dm.cn.db.model.TableInfo;
import com.dm.cn.db.service.ColumnInfoService;
import com.dm.cn.db.service.ConstraintService;
import com.dm.cn.db.service.TableInfoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.List;

@SpringBootApplication
public class DbApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(DbApplication.class, args);

		TableInfoService tableInfoService = context.getBean(TableInfoService.class);
		ColumnInfoService columnService = context.getBean(ColumnInfoService.class);
		ConstraintService constraintService = context.getBean(ConstraintService.class);

		// 指定要查询的schema名称
		String targetSchema = "school_schema";

		// 输出表结构信息
		List<TableInfo> tables = tableInfoService.getTableInfo(targetSchema);
		System.out.println("在schema: " + targetSchema + " 中找到 " + tables.size() + " 张表：");
		for (TableInfo table : tables) {
			System.out.println("表名: " + table.getId().getTable_name() + "，类型: " + table.getTable_type());

			String targetTable = table.getId().getTable_name();

			// 输出字段信息
			List<ColumnInfo> columns = columnService.getColumns(targetSchema, targetTable);
			System.out.println("表 " + targetSchema + "." + targetTable + " 的字段信息：");
			System.out.println("序号 | 字段名 | 数据类型 | 是否可为空");
			System.out.println("-------------------------------------");
			for (ColumnInfo column : columns) {
				System.out.printf("%d | %s | %s | %s%n",
						column.getOrdinal_position(),
						column.getId().getColumn_name(),
						column.getData_type(),
						column.getIs_nullable());
			}

			// 输出约束信息
			List<TableConstraint> constraints = constraintService.getConstraints(targetSchema, targetTable);
			if (constraints.isEmpty()) {
				System.out.println("表 " + targetSchema + "." + targetTable + " 没有约束");
				return;
			}
			System.out.println("表 " + targetSchema + "." + targetTable + " 的主外键关系：");
			for (TableConstraint constraint : constraints) {
				String constraintName = constraint.getConstraint_name();
				String constraintType = constraint.getConstraint_type();
				if ("PRIMARY KEY".equals(constraintType)) {
					System.out.printf("主键约束：%s，字段：%s%n", constraintName, constraint.getPrimary_column());
				} else if ("FOREIGN KEY".equals(constraintType)) {
					System.out.printf("外键约束：%s，表：%s，字段：%s -> 关联表：%s，关联字段：%s%n",
							constraintName,
							constraint.getPrimary_table(),
							constraint.getPrimary_column(),
							constraint.getForeign_table(),
							constraint.getForeign_column());
				}
			}
		}
	}

}
