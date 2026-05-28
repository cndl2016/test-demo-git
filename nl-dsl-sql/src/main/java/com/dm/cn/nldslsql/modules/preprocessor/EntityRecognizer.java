package com.dm.cn.nldslsql.modules.preprocessor;

import java.util.*;

/**
 * 实体识别器
 * 从自然语言查询中识别涉及的表名和字段名
 * 支持直接表名匹配和中文关键词映射
 */
public class EntityRecognizer {
    /** 中文关键词到表名的映射 */
    private final Map<String, String> tableKeywordMap = new HashMap<>();

    public EntityRecognizer() {
        // 初始化常见中文关键词到表名的映射
        tableKeywordMap.put("用户", "users");
        tableKeywordMap.put("会员", "users");
        tableKeywordMap.put("订单", "orders");
        tableKeywordMap.put("商品", "products");
        tableKeywordMap.put("产品", "products");
        tableKeywordMap.put("明细", "order_items");
        tableKeywordMap.put("地址", "addresses");
        tableKeywordMap.put("收货地址", "addresses");
        tableKeywordMap.put("分类", "categories");
        tableKeywordMap.put("商品分类", "categories");
        tableKeywordMap.put("品牌", "brands");
        tableKeywordMap.put("SKU", "product_skus");
        tableKeywordMap.put("规格", "product_skus");
        tableKeywordMap.put("支付", "payments");
        tableKeywordMap.put("付款", "payments");
        tableKeywordMap.put("优惠券", "coupons");
        tableKeywordMap.put("评价", "product_reviews");
        tableKeywordMap.put("评论", "product_reviews");
        tableKeywordMap.put("购物车", "shopping_carts");
        tableKeywordMap.put("收藏", "product_favorites");
        tableKeywordMap.put("秒杀", "flash_sales");
        tableKeywordMap.put("限时抢购", "flash_sales");
        tableKeywordMap.put("行为", "user_behaviors");
        tableKeywordMap.put("用户行为", "user_behaviors");
        tableKeywordMap.put("标签", "product_tags");
        tableKeywordMap.put("商品标签", "product_tags");
        tableKeywordMap.put("管理员", "admin_users");
        tableKeywordMap.put("后台用户", "admin_users");
        tableKeywordMap.put("日志", "admin_logs");
        tableKeywordMap.put("操作日志", "admin_logs");
        tableKeywordMap.put("库存", "stock_logs");
        tableKeywordMap.put("库存流水", "stock_logs");
        tableKeywordMap.put("配送", "shipping_methods");
        tableKeywordMap.put("配送方式", "shipping_methods");
        tableKeywordMap.put("仓库", "warehouses");
        tableKeywordMap.put("供应商", "product_suppliers");
        tableKeywordMap.put("采购", "product_purchase_prices");
        tableKeywordMap.put("采购价", "product_purchase_prices");
    }

    /**
     * 识别查询中涉及的表名
     * @param query 查询字符串
     * @param schema 数据库schema
     * @return 识别出的表名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> recognizeTables(String query, Map<String, Object> schema) {
        List<String> recognized = new ArrayList<>();
        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        if (tablesDict == null) return recognized;

        List<String> availableTables = new ArrayList<>(tablesDict.keySet());
        String queryLower = query.toLowerCase();

        // 首先尝试直接匹配表名
        for (String table : availableTables) {
            if (queryLower.contains(table.toLowerCase())) {
                recognized.add(table);
            }
        }

        // 如果没有直接匹配，尝试中文关键词映射
        if (recognized.isEmpty()) {
            for (Map.Entry<String, String> entry : tableKeywordMap.entrySet()) {
                if (query.contains(entry.getKey()) && availableTables.contains(entry.getValue())) {
                    recognized.add(entry.getValue());
                }
            }
        }

        return recognized;
    }

    /**
     * 识别查询中涉及的字段名
     * @param query 查询字符串
     * @param schema 数据库schema
     * @return 识别出的字段名列表
     */
    @SuppressWarnings("unchecked")
    public List<String> recognizeFields(String query, Map<String, Object> schema) {
        List<String> recognized = new ArrayList<>();
        List<String> allFields = new ArrayList<>();

        Map<String, Object> tablesDict = (Map<String, Object>) schema.get("tables");
        if (tablesDict == null) return recognized;

        // 收集所有表的字段名（包括带表名前缀和不带前缀的）
        for (Map.Entry<String, Object> entry : tablesDict.entrySet()) {
            String table = entry.getKey();
            Map<String, Object> tableInfo = (Map<String, Object>) entry.getValue();
            List<String> fields = (List<String>) tableInfo.get("columns");
            if (fields != null) {
                for (String field : fields) {
                    allFields.add(table + "." + field);
                    allFields.add(field);
                }
            }
        }

        String queryLower = query.toLowerCase();
        for (String field : allFields) {
            if (queryLower.contains(field.toLowerCase())) {
                recognized.add(field);
            }
        }

        return recognized;
    }
}
