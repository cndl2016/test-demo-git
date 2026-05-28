package com.dm.cn.stock.repository;

import com.dm.cn.stock.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 股票基本信息数据访问层
 * 提供对stock表的CRUD操作
 */
@Repository
public interface StockRepository extends JpaRepository<StockEntity, String> {
}
