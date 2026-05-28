package com.dm.cn.stock.vo;

import lombok.Data;

/**
 * 股票历史数据查询请求参数
 */
@Data
public class StockHistoryRequest {
    /** 股票代码，如 600519、000001，若 stockCode 为空，则同步 stock 表中所有股票在指定日期范围内的历史价格 */
    private String stockCode;
    /** 查询开始日期，格式 yyyy-MM-dd */
    private String startDate;
    /** 查询结束日期，格式 yyyy-MM-dd，为空时默认为上一个交易日 */
    private String endDate;
}
