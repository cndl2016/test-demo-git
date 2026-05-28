package com.dm.cn.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 相似度工具类：命名相似度、值域计算
 */
public class SimilarityUtil {

    private static final Map<String, String> ABBREVIATION_MAP = new HashMap<>();
    static {
        ABBREVIATION_MAP.put("usr", "user");
        ABBREVIATION_MAP.put("user", "user");
        ABBREVIATION_MAP.put("emp", "employee");
        ABBREVIATION_MAP.put("cust", "customer");
        ABBREVIATION_MAP.put("cli", "client");
        ABBREVIATION_MAP.put("addr", "address");
        ABBREVIATION_MAP.put("dept", "department");
        ABBREVIATION_MAP.put("org", "organization");
        ABBREVIATION_MAP.put("prod", "product");
        ABBREVIATION_MAP.put("ord", "order");
        ABBREVIATION_MAP.put("item", "item");
        ABBREVIATION_MAP.put("no", "number");
        ABBREVIATION_MAP.put("id", "id");
        ABBREVIATION_MAP.put("code", "code");
        ABBREVIATION_MAP.put("key", "key");
    }

    private static final Set<String> KEY_TOKENS = new HashSet<>(Arrays.asList(
            "id", "code", "no", "key", "num", "seq"
    ));

    // ===================== 【主入口】 =====================
    public static double fkColumnNameSimilarity(String fkTable, String fkCol, String pkTable, String pkCol) {
        List<String> pkCandidateNames = generatePkCandidateNames(pkTable, pkCol);
        String fkStandard = normalize(fkCol);
        double maxScore = 0.0;
        for (String pkCandidate : pkCandidateNames) {
            double score = singlePairSimilarity(fkStandard, pkCandidate);
            maxScore = Math.max(maxScore, score);
        }
        return maxScore;
    }

    private static List<String> generatePkCandidateNames(String tableName, String colName) {
        List<String> result = new ArrayList<>();
        String table = normalize(tableName);
        result.add(normalize(colName));
        if (KEY_TOKENS.contains(colName.toLowerCase())) {
            result.add(table + "_" + colName);
            result.add(camelCase(table, colName));
            result.add(table + colName);
        }
        return result;
    }

    private static double singlePairSimilarity(String s1, String s2) {
        List<String> t1 = splitAndExpand(s1);
        List<String> t2 = splitAndExpand(s2);
        double keyScore = calculateKeyTokenScore(t1, t2);
        double jaccard = jaccardSimilarity(t1, t2);
        double editSim = editDistanceSimilarity(s1, s2);
        return 0.4 * keyScore + 0.4 * jaccard + 0.2 * editSim;
    }

    private static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[0-9]", "")
                .replaceAll("_+", "_")
                .replaceAll("[-\\s]", "_");
    }

    private static String camelCase(String table, String col) {
        return table + col.substring(0, 1).toUpperCase() + col.substring(1);
    }

    private static List<String> splitAndExpand(String s) {
        List<String> tokens = new ArrayList<>();
        if (s.isBlank()) return tokens;
        for (String t : s.split("_")) {
            if (t.isBlank()) continue;
            String expanded = ABBREVIATION_MAP.getOrDefault(t, t);
            tokens.add(expanded);
        }
        return tokens.stream().filter(t -> !t.isBlank()).collect(Collectors.toList());
    }

    private static double calculateKeyTokenScore(List<String> t1, List<String> t2) {
        Set<String> k1 = new HashSet<>();
        Set<String> k2 = new HashSet<>();
        for (String t : t1) if (KEY_TOKENS.contains(t)) k1.add(t);
        for (String t : t2) if (KEY_TOKENS.contains(t)) k2.add(t);
        if (k1.isEmpty() && k2.isEmpty()) return 1.0;
        Set<String> intersection = new HashSet<>(k1);
        intersection.retainAll(k2);
        return intersection.isEmpty() ? 0.0 : 1.0;
    }

    private static double jaccardSimilarity(Collection<String> a, Collection<String> b) {
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 1.0;
        Set<String> intersect = new HashSet<>(a);
        intersect.retainAll(b);
        return (double) intersect.size() / union.size();
    }

    public static double editDistanceSimilarity(String s1, String s2) {
        int distance = editDistance(s1.toLowerCase(), s2.toLowerCase());
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 1.0 : 1 - (double) distance / maxLen;
    }

    private static int editDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    // ===================== 联合键命名相似度 =====================
    public static double compositeColumnNameSimilarity(List<String> fkCols, List<String> pkCols) {
        if (fkCols.size() != pkCols.size()) return 0.0;
        double total = 0.0;
        for (int i = 0; i < fkCols.size(); i++) {
            total += editDistanceSimilarity(fkCols.get(i), pkCols.get(i));
        }
        return total / fkCols.size();
    }

    // ===================== 值域重合度计算 =====================
    public static double valueRangeSimilarity(long intersection, long union) {
        if (union == 0) return 0;
        return (double) intersection / union;
    }

    // ===================== 【对外唯一入口】 =====================
    public static double calculate(List<Object> valuesA, List<Object> valuesB, String dataType) {
        if (valuesA.isEmpty() || valuesB.isEmpty()) return 0.0;
        if (isNumericType(dataType)) {
            return calculateNumericWasserstein(valuesA, valuesB);
        } else {
            return calculateCategoricalFrequencySimilarity(valuesA, valuesB);
        }
    }

    // ===================== 联合键分布相似度入口 =====================
    public static double calculateComposite(List<Map<String, Object>> valuesA, List<Map<String, Object>> valuesB) {
        if (valuesA.isEmpty() || valuesB.isEmpty()) return 0.0;
        List<Object> concatA = valuesA.stream()
                .map(m -> m.values().stream().map(String::valueOf).collect(Collectors.joining("|")))
                .collect(Collectors.toList());
        List<Object> concatB = valuesB.stream()
                .map(m -> m.values().stream().map(String::valueOf).collect(Collectors.joining("|")))
                .collect(Collectors.toList());
        return calculateCategoricalFrequencySimilarity(concatA, concatB);
    }

    // ===================== 数值型分布：Wasserstein 距离 =====================
    private static double calculateNumericWasserstein(List<Object> a, List<Object> b) {
        try {
            List<Double> listA = convertToDouble(a);
            List<Double> listB = convertToDouble(b);
            listA = normalize(listA);
            listB = normalize(listB);
            Collections.sort(listA);
            Collections.sort(listB);
            double[] ecdfA = computeEcdf(listA, 100);
            double[] ecdfB = computeEcdf(listB, 100);
            double distance = wassersteinDistance(ecdfA, ecdfB);
            return Math.max(0, 1.0 - distance);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ===================== 分类型分布：频率分布相似度 =====================
    private static double calculateCategoricalFrequencySimilarity(List<Object> a, List<Object> b) {
        Map<Object, Double> freqA = computeFrequency(a);
        Map<Object, Double> freqB = computeFrequency(b);
        double common = 0.0;
        for (Object key : freqA.keySet()) {
            if (freqB.containsKey(key)) {
                common += Math.min(freqA.get(key), freqB.get(key));
            }
        }
        double totalA = freqA.values().stream().mapToDouble(d -> d).sum();
        double totalB = freqB.values().stream().mapToDouble(d -> d).sum();
        return common / Math.max(totalA, totalB);
    }

    private static boolean isNumericType(String dataType) {
        String lower = dataType.toLowerCase();
        return lower.contains("int") || lower.contains("float") || lower.contains("double")
                || lower.contains("numeric") || lower.contains("decimal")
                || lower.contains("real") || lower.contains("bigint");
    }

    private static List<Double> convertToDouble(List<Object> list) {
        List<Double> res = new ArrayList<>();
        for (Object o : list) {
            if (o == null) continue;
            try {
                res.add(Double.parseDouble(o.toString()));
            } catch (Exception ignored) {}
        }
        return res;
    }

    private static List<Double> normalize(List<Double> list) {
        double min = Collections.min(list);
        double max = Collections.max(list);
        if (max == min) return list.stream().map(v -> 0.0).collect(Collectors.toList());
        return list.stream().map(v -> (v - min) / (max - min)).collect(Collectors.toList());
    }

    private static double[] computeEcdf(List<Double> sorted, int bins) {
        double[] ecdf = new double[bins];
        int n = sorted.size();
        for (int i = 0; i < bins; i++) {
            double q = (i + 1) / (double) bins;
            int idx = (int) (q * n);
            idx = Math.min(idx, n - 1);
            ecdf[i] = sorted.get(idx);
        }
        return ecdf;
    }

    private static double wassersteinDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum / a.length;
    }

    private static Map<Object, Double> computeFrequency(List<Object> list) {
        Map<Object, Long> count = new HashMap<>();
        for (Object o : list) {
            if (o == null) continue;
            count.put(o, count.getOrDefault(o, 0L) + 1);
        }
        long total = list.size();
        Map<Object, Double> freq = new HashMap<>();
        for (Map.Entry<Object, Long> entry : count.entrySet()) {
            freq.put(entry.getKey(), entry.getValue() / (double) total);
        }
        return freq;
    }
}
