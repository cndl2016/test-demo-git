package com.dm.cn.stock.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 股票基本信息数据库实体
 * 对应SQLite中的stock表，记录每只股票的基础资料及实时行情快照
 */
@Entity
@Table(name = "stock")
@Data
public class StockEntity {

    /** 股票代码，主键 */
    @Id
    private String stockCode;

    /** 股票名称 */
    private String stockName;

    /** 所属市场（sh-上海，sz-深圳） */
    private String market;

    /** 当前价格（最新价） */
    private Double currentPrice;

    /** 昨日收盘价 */
    private Double previousClose;

    /** 今日开盘价 */
    private Double openPrice;

    /** 今日最高价 */
    private Double highPrice;

    /** 今日最低价 */
    private Double lowPrice;

    /** 成交量（手） */
    private Long volume;

    /** 涨跌额 */
    private Double changeAmount;

    /** 涨跌幅（%） */
    private Double changePercent;

    /** 换手率（%） */
    private Double turnover;

    /** 市盈率（PE） */
    private Double peRatio;

    /** 市净率（PB） */
    private Double pbRatio;

    /** 振幅（%） */
    private Double amplitude;

    /** 总市值（万元） */
    private Double totalMarketCap;

    /** 流通市值（万元） */
    private Double floatMarketCap;

    /** 涨停价 */
    private Double limitUp;

    /** 跌停价 */
    private Double limitDown;
}
