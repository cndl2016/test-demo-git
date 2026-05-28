package com.dm.cn.stock.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 交易日工具类
 * 提供交易日期的计算和判断功能
 * 注：当前实现仅跳过周末，未处理法定节假日
 */
public class TradeDateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取今天上一个交易日
     * 从当前日期往前推，跳过周六和周日
     *
     * @return 上一个交易日的日期字符串，格式 yyyy-MM-dd
     */
    public static String getLastTradeDate() {
        LocalDate date = LocalDate.now();
        date = date.minusDays(1);
        // 如果昨天是周末，继续往前推
        while (isWeekend(date)) {
            date = date.minusDays(1);
        }
        return date.format(FORMATTER);
    }

    /**
     * 判断指定日期是否为周末
     *
     * @param date 日期
     * @return true 表示是周六或周日
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 判断指定日期是否为交易日（非周末）
     * 注：未排除法定节假日
     *
     * @param dateStr 日期字符串，格式 yyyy-MM-dd
     * @return true 表示是交易日
     */
    public static boolean isTradeDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, FORMATTER);
        return !isWeekend(date);
    }

    /**
     * 获取指定日期当日或之后的第一个交易日
     * 如果指定日期本身是周末，则向后推移到第一个非周末日期
     *
     * @param dateStr 日期字符串，格式 yyyy-MM-dd
     * @return 当日或之后的第一个交易日
     */
    public static String getFirstTradeDateOnOrAfter(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, FORMATTER);
        while (isWeekend(date)) {
            date = date.plusDays(1);
        }
        return date.format(FORMATTER);
    }
}
