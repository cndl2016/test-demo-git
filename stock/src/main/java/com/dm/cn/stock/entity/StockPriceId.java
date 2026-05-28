package com.dm.cn.stock.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 股票价格复合主键类
 * 由股票代码和交易日期共同组成唯一标识
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceId implements Serializable {
    /** 股票代码 */
    private String stockCode;
    /** 交易日期 */
    private String date;
}
