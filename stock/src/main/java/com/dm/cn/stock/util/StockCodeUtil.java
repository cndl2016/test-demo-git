package com.dm.cn.stock.util;

/**
 * 股票代码转换工具类
 * 负责将纯数字股票代码转换为各数据平台所需的格式
 */
public class StockCodeUtil {

    /**
     * 将股票代码转换为腾讯财经接口格式
     * 上海股票（6/5开头）加 "sh" 前缀，深圳股票加 "sz" 前缀
     *
     * @param stockCode 纯数字股票代码，如 600519
     * @return 带市场前缀的代码，如 sh600519
     */
    public static String convertToTencentSymbol(String stockCode) {
        if (stockCode == null || stockCode.isEmpty()) {
            throw new IllegalArgumentException("股票代码不能为空");
        }
        String code = stockCode.trim();
        // 上海主板/科创板/基金以 6 或 5 开头
        if (code.startsWith("6") || code.startsWith("5")) {
            return "sh" + code;
        } else {
            // 深圳主板/创业板/北交所以 0/3/4/8 开头
            return "sz" + code;
        }
    }
}
