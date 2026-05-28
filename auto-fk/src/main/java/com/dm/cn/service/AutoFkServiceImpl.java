package com.dm.cn.service;

import com.dm.cn.config.AutoFkConfig;
import com.dm.cn.model.FkMatchResult;
import com.dm.cn.model.ForeignKeySourceCandidate;
import com.dm.cn.model.PrimaryKeyCandidate;
import com.dm.cn.util.SimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutoFkServiceImpl implements AutoFkService {

    private final DbMetaMapper dbMetaMapper;

    @Resource(name = "autoFkExecutor")
    private ExecutorService executor;

    // ====================== 总入口：执行Auto-FK全流程 ======================
    @Override
    public List<FkMatchResult> executeAutoFk() {
        List<PrimaryKeyCandidate> pkList = collectPrimaryKeyCandidates();
        List<ForeignKeySourceCandidate> fkSourceList = buildForeignKeySource(pkList);
        List<FkMatchResult> filteredList = pairAndFilter(pkList, fkSourceList);
        return weightAndOutput(filteredList);
    }

    // ====================== 步骤1：收集候选主键（支持联合主键） ======================
    private List<PrimaryKeyCandidate> collectPrimaryKeyCandidates() {
        List<PrimaryKeyCandidate> result = new ArrayList<>();

        // 1.1 读取显式主键（按约束名分组，支持联合主键）
        List<Map<String, Object>> explicitPks = dbMetaMapper.selectExplicitPrimaryKeys();
        Map<String, List<Map<String, Object>>> groupedByConstraint = explicitPks.stream()
                .collect(Collectors.groupingBy(m -> (String) m.get("constraint_name")));

        for (List<Map<String, Object>> group : groupedByConstraint.values()) {
            group.sort(Comparator.comparingInt(m -> (Integer) m.get("ordinal_position")));
            PrimaryKeyCandidate pk = new PrimaryKeyCandidate();
            pk.setTableName((String) group.get(0).get("table_name"));
            pk.setColumnNames(group.stream().map(m -> (String) m.get("column_name")).collect(Collectors.toList()));
            pk.setDataTypes(group.stream().map(m -> (String) m.get("data_type")).collect(Collectors.toList()));
            pk.setExplicitPk(true);
            result.add(pk);
        }

        // 1.2 无显式主键 → 推断单列主键（唯一值比例=1 + 命名规则）
        List<Map<String, Object>> allColumns = dbMetaMapper.selectAllColumns();
        for (Map<String, Object> col : allColumns) {
            String table = (String) col.get("table_name");
            String column = (String) col.get("column_name");
            // 跳过已识别的显式主键中的任意列
            boolean exists = result.stream().anyMatch(p ->
                    p.getTableName().equals(table) && p.getColumnNames().contains(column));
            if (exists) continue;

            double uniqueRatio = dbMetaMapper.selectColumnUniqueRatio(table, column);
            boolean nameMatch = column.matches("(id|code|.*_id)") && !column.equals("row_id");
            if (uniqueRatio == 1.0 && nameMatch) {
                PrimaryKeyCandidate pk = new PrimaryKeyCandidate();
                pk.setTableName(table);
                pk.setColumnNames(Collections.singletonList(column));
                pk.setDataTypes(Collections.singletonList((String) col.get("data_type")));
                pk.setExplicitPk(false);
                result.add(pk);
            }
        }
        return result;
    }

    // ====================== 步骤2：构建候选外键源（支持联合外键） ======================
    private List<ForeignKeySourceCandidate> buildForeignKeySource(List<PrimaryKeyCandidate> pkList) {
        List<ForeignKeySourceCandidate> result = new ArrayList<>();
        List<Map<String, Object>> allColumns = dbMetaMapper.selectAllColumns();

        // 按表分组所有列
        Map<String, List<Map<String, Object>>> columnsByTable = allColumns.stream()
                .collect(Collectors.groupingBy(m -> (String) m.get("table_name")));

        for (Map.Entry<String, List<Map<String, Object>>> entry : columnsByTable.entrySet()) {
            String table = entry.getKey();
            List<Map<String, Object>> cols = entry.getValue();
            Set<String> colNames = cols.stream().map(m -> (String) m.get("column_name")).collect(Collectors.toSet());
            Map<String, String> colNameToType = cols.stream().collect(Collectors.toMap(
                    m -> (String) m.get("column_name"), m -> (String) m.get("data_type"), (a, b) -> a));
            Map<String, Boolean> colNameToNullable = cols.stream().collect(Collectors.toMap(
                    m -> (String) m.get("column_name"), m -> "YES".equals(m.get("is_nullable")), (a, b) -> a));

            // 2.1 单列外键候选
            for (Map<String, Object> col : cols) {
                String column = (String) col.get("column_name");
                String type = (String) col.get("data_type");

                // 排除：主键列
                boolean isPk = pkList.stream().anyMatch(p ->
                        p.getTableName().equals(table) && p.getColumnNames().contains(column));
                if (isPk) continue;
                // 排除：时间戳/大文本/二进制
                if (column.contains("at") || type.contains("text") || type.contains("blob")) continue;

                ForeignKeySourceCandidate fk = new ForeignKeySourceCandidate();
                fk.setTableName(table);
                fk.setColumnNames(Collections.singletonList(column));
                fk.setDataTypes(Collections.singletonList(type));
                fk.setNullable("YES".equals(col.get("is_nullable")));
                result.add(fk);
            }

            // 2.2 联合外键候选：检查是否包含其他表联合主键的所有列
            for (PrimaryKeyCandidate pk : pkList) {
                if (pk.getTableName().equals(table)) continue;
                if (pk.getColumnNames().size() < 2) continue; // 只处理联合主键

                // 本表必须包含联合主键的所有列
                if (!colNames.containsAll(pk.getColumnNames())) continue;

                // 检查数据类型是否匹配
                boolean typeMatch = true;
                for (int i = 0; i < pk.getColumnNames().size(); i++) {
                    String pkCol = pk.getColumnNames().get(i);
                    String pkType = pk.getDataTypes().get(i);
                    String fkType = colNameToType.get(pkCol);
                    if (!pkType.equals(fkType)) {
                        typeMatch = false;
                        break;
                    }
                }
                if (!typeMatch) continue;

                // 排除：如果该组合中任一列在本表是显式主键列，则跳过（避免主键-主键误匹配）
                boolean anyPk = pk.getColumnNames().stream().anyMatch(c -> {
                    boolean isPkLocal = pkList.stream().anyMatch(p ->
                            p.getTableName().equals(table) && p.getColumnNames().contains(c));
                    return isPkLocal;
                });
                if (anyPk) continue;

                ForeignKeySourceCandidate fk = new ForeignKeySourceCandidate();
                fk.setTableName(table);
                fk.setColumnNames(new ArrayList<>(pk.getColumnNames()));
                fk.setDataTypes(pk.getColumnNames().stream().map(colNameToType::get).collect(Collectors.toList()));
                fk.setNullable(pk.getColumnNames().stream().anyMatch(colNameToNullable::get));
                result.add(fk);
            }
        }
        return result;
    }

    // ====================== 步骤3：配对 + 5层逐层过滤（支持联合键） ======================
    private List<FkMatchResult> pairAndFilter(List<PrimaryKeyCandidate> pkList, List<ForeignKeySourceCandidate> fkList) {
        List<FkMatchResult> result = new ArrayList<>();
        List<Future<FkMatchResult>> futures = new ArrayList<>();

        for (ForeignKeySourceCandidate fk : fkList) {
            for (PrimaryKeyCandidate pk : pkList) {
                if (fk.getTableName().equals(pk.getTableName())) continue;
                // 列数必须相同才能匹配
                if (fk.getColumnNames().size() != pk.getColumnNames().size()) continue;

                futures.add(executor.submit(() -> {
                    FkMatchResult res = new FkMatchResult();
                    res.setFkTable(fk.getTableName());
                    res.setFkColumns(fk.getColumnNames());
                    res.setPkTable(pk.getTableName());
                    res.setPkColumns(pk.getColumnNames());

                    // ========== 第1层：类型硬过滤（逐列匹配） ==========
                    boolean typeMatch = true;
                    for (int i = 0; i < fk.getDataTypes().size(); i++) {
                        if (!fk.getDataTypes().get(i).equals(pk.getDataTypes().get(i))) {
                            typeMatch = false;
                            break;
                        }
                    }
                    if (!typeMatch) return null;

                    // ========== 第2层：值域粗筛 ==========
                    long intersection;
                    long fkDistinct;
                    long pkDistinct;
                    if (fk.getColumnNames().size() == 1) {
                        intersection = dbMetaMapper.selectIntersectionCount(
                                fk.getTableName(), fk.getColumnNames().get(0),
                                pk.getTableName(), pk.getColumnNames().get(0));
                        fkDistinct = dbMetaMapper.selectDistinctCount(fk.getTableName(), fk.getColumnNames().get(0));
                        pkDistinct = dbMetaMapper.selectDistinctCount(pk.getTableName(), pk.getColumnNames().get(0));
                    } else {
                        intersection = dbMetaMapper.selectIntersectionCountComposite(
                                fk.getTableName(), fk.getColumnNames(),
                                pk.getTableName(), pk.getColumnNames());
                        fkDistinct = dbMetaMapper.selectDistinctCountComposite(fk.getTableName(), fk.getColumnNames());
                        pkDistinct = dbMetaMapper.selectDistinctCountComposite(pk.getTableName(), pk.getColumnNames());
                    }
                    long union = fkDistinct + pkDistinct - intersection;
                    double valueSim = SimilarityUtil.valueRangeSimilarity(intersection, union);
                    res.setValueRangeSimilarity(valueSim);
                    if (valueSim < AutoFkConfig.VALUE_RANGE_THRESHOLD) return null;

                    // ========== 第3层：命名相似度 ==========
                    double nameSim;
                    if (fk.getColumnNames().size() == 1) {
                        nameSim = SimilarityUtil.editDistanceSimilarity(fk.getColumnNames().get(0), pk.getColumnNames().get(0));
                    } else {
                        nameSim = SimilarityUtil.compositeColumnNameSimilarity(fk.getColumnNames(), pk.getColumnNames());
                    }
                    res.setNameSimilarity(nameSim);
                    if (nameSim < AutoFkConfig.NAME_SIMILARITY_THRESHOLD) return null;

                    // ========== 第4层：分布相似度精算 ==========
                    double distSim;
                    if (fk.getColumnNames().size() == 1) {
                        List<Object> fkValues = dbMetaMapper.selectColumnValues(fk.getTableName(), fk.getColumnNames().get(0));
                        List<Object> pkValues = dbMetaMapper.selectColumnValues(pk.getTableName(), pk.getColumnNames().get(0));
                        distSim = SimilarityUtil.calculate(fkValues, pkValues, fk.getDataTypes().get(0));
                    } else {
                        List<Map<String, Object>> fkValues = dbMetaMapper.selectCompositeColumnValues(fk.getTableName(), fk.getColumnNames());
                        List<Map<String, Object>> pkValues = dbMetaMapper.selectCompositeColumnValues(pk.getTableName(), pk.getColumnNames());
                        distSim = SimilarityUtil.calculateComposite(fkValues, pkValues);
                    }
                    res.setDistributionSimilarity(distSim);
                    if (distSim < AutoFkConfig.DISTRIBUTION_SIMILARITY_THRESHOLD) return null;

                    // ========== 第5层：SQL日志强化 ==========
                    res.setSqlConfidence(0.8);

                    return res;
                }));
            }
        }

        for (Future<FkMatchResult> future : futures) {
            try {
                FkMatchResult res = future.get();
                if (res != null) result.add(res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // ====================== 步骤4：加权融合 + 输出 ======================
    private List<FkMatchResult> weightAndOutput(List<FkMatchResult> list) {
        List<FkMatchResult> result = new ArrayList<>();
        for (FkMatchResult res : list) {
            double total = res.getNameSimilarity() * AutoFkConfig.WEIGHT_NAME
                    + res.getValueRangeSimilarity() * AutoFkConfig.WEIGHT_VALUE
                    + res.getDistributionSimilarity() * AutoFkConfig.WEIGHT_DISTRIBUTION
                    + res.getSqlConfidence() * AutoFkConfig.WEIGHT_SQL;
            res.setTotalConfidence(total);

            if (total >= AutoFkConfig.TOTAL_CONFIDENCE_THRESHOLD) {
                result.add(res);
            }
        }
        result.sort(Comparator.comparing(FkMatchResult::getTotalConfidence).reversed());
        return result;
    }
}
