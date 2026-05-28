package com.dm.cn.stock.repository;

import com.dm.cn.stock.entity.StockPriceEntity;
import com.dm.cn.stock.entity.StockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票价格数据访问层
 * 提供对stock_price表的CRUD操作和自定义查询
 */
@Repository
public interface StockPriceRepository extends JpaRepository<StockPriceEntity, StockPriceId> {

    /**
     * 根据股票代码和日期范围查询历史价格
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该日期范围内的股票价格列表，按日期升序排列
     */
    List<StockPriceEntity> findByStockCodeAndDateBetweenOrderByDateAsc(
            String stockCode, String startDate, String endDate);

    /**
     * 查询某只股票在数据库中最早的一条记录
     *
     * @param stockCode 股票代码
     * @return 日期最早的股票价格记录
     */
    Optional<StockPriceEntity> findFirstByStockCodeOrderByDateAsc(String stockCode);

    /**
     * 查询某只股票在数据库中最晚的一条记录
     *
     * @param stockCode 股票代码
     * @return 日期最晚的股票价格记录
     */
    Optional<StockPriceEntity> findFirstByStockCodeOrderByDateDesc(String stockCode);

    /**
     * 根据股票代码删除所有历史价格记录
     *
     * @param stockCode 股票代码
     */
    void deleteByStockCode(String stockCode);
}
