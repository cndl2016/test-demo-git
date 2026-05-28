package com.dm.cn.stock.vo;

import lombok.Data;

/**
 * 股票价格信息视图对象
 * 封装单日的股票行情数据
 */
@Data
public class StockPriceVO {
    /** 股票代码，如 600519 */
    private String stockCode;
    /** 交易日期，格式 yyyy-MM-dd */
    private String date;
    /** 开盘价 */
    private Double open;
    /** 收盘价 */
    private Double close;
    /** 最高价 */
    private Double high;
    /** 最低价 */
    private Double low;
    /** 成交量（股） */
    private Long volume;
    /** 成交额（元），腾讯接口不返回此字段 */
    private Double amount;
    /** 涨跌额，腾讯接口不返回此字段 */
    private Double changeAmount;
    /** 涨跌幅（%），腾讯接口不返回此字段 */
    private Double changePercent;
}
