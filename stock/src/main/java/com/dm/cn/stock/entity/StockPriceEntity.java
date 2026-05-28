package com.dm.cn.stock.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 股票价格数据库实体
 * 对应SQLite中的stock_price表，记录每只股票每日的行情数据
 */
@Entity
@Table(name = "stock_price")
@IdClass(StockPriceId.class)
@Data
public class StockPriceEntity {

    /** 股票代码，复合主键之一 */
    @Id
    private String stockCode;

    /** 交易日期，格式yyyy-MM-dd，复合主键之一 */
    @Id
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
}
