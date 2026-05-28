package com.dm.cn.stock.converter;

import com.dm.cn.stock.entity.StockPriceEntity;
import com.dm.cn.stock.vo.StockPriceVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 股票价格对象转换器
 * 负责在Entity（数据库实体）和VO（视图对象）之间进行转换
 */
public class StockPriceConverter {

    /**
     * 将VO转换为Entity
     *
     * @param vo        视图对象
     * @param stockCode 股票代码
     * @return 数据库实体
     */
    public static StockPriceEntity toEntity(StockPriceVO vo, String stockCode) {
        if (vo == null) {
            return null;
        }
        StockPriceEntity entity = new StockPriceEntity();
        entity.setStockCode(stockCode);
        entity.setDate(vo.getDate());
        entity.setOpen(vo.getOpen());
        entity.setClose(vo.getClose());
        entity.setHigh(vo.getHigh());
        entity.setLow(vo.getLow());
        entity.setVolume(vo.getVolume());
        return entity;
    }

    /**
     * 将Entity转换为VO
     *
     * @param entity 数据库实体
     * @return 视图对象
     */
    public static StockPriceVO toVo(StockPriceEntity entity) {
        if (entity == null) {
            return null;
        }
        StockPriceVO vo = new StockPriceVO();
        vo.setStockCode(entity.getStockCode());
        vo.setDate(entity.getDate());
        vo.setOpen(entity.getOpen());
        vo.setClose(entity.getClose());
        vo.setHigh(entity.getHigh());
        vo.setLow(entity.getLow());
        vo.setVolume(entity.getVolume());
        return vo;
    }

    /**
     * 将Entity列表转换为VO列表
     *
     * @param entityList 数据库实体列表
     * @return 视图对象列表
     */
    public static List<StockPriceVO> toVoList(List<StockPriceEntity> entityList) {
        List<StockPriceVO> voList = new ArrayList<>();
        for (StockPriceEntity entity : entityList) {
            StockPriceVO vo = toVo(entity);
            if (vo != null) {
                voList.add(vo);
            }
        }
        return voList;
    }
}
