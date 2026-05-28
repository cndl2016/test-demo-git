package com.dm.cn.stock.controller;

import com.dm.cn.stock.entity.StockEntity;
import com.dm.cn.stock.service.StockService;
import com.dm.cn.stock.vo.StockHistoryRequest;
import com.dm.cn.stock.vo.StockPriceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 股票数据查询接口
 * 提供股票历史价格查询、基本信息查询等RESTful API
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /**
     * 同步所有A股股票基本信息
     * 从东方财富获取全量列表，再分批调用腾讯接口补全详细字段后入库
     *
     * @return 同步结果，包含同步的股票数量
     */
    @GetMapping("/info/all")
    public Map<String, Object> syncAllStockInfo() {
        int count = stockService.syncAllStockInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "同步完成");
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        result.put("data", data);
        return result;
    }

    /**
     * 获取所有股票今日实时行情
     * 从stock表读取全部股票列表，调用腾讯接口获取今日数据后保存到stock_price表并返回
     *
     * @return 所有股票的今日价格信息列表
     */
    @GetMapping("/today")
    public Map<String, Object> getAllTodayInfo() {
        List<StockPriceVO> data = stockService.getAllTodayInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    /**
     * 查询股票基本信息
     *
     * @param stockCode 股票代码
     * @return 股票基本信息
     */
    @GetMapping("/info/{stockCode}")
    public Map<String, Object> getStockInfo(@PathVariable("stockCode") String stockCode) {
        StockEntity stock = stockService.getStockInfo(stockCode);

        Map<String, Object> result = new HashMap<>();
        if (stock == null) {
            result.put("code", 404);
            result.put("message", "股票不存在");
        } else {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", stock);
        }
        return result;
    }

    /**
     * 查询股票历史价格
     *
     * @param request 包含股票代码和开始日期的请求参数
     * @return 该股票（或所有股票）从开始日期到结束日期的所有日线数据
     */
    @PostMapping("/history")
    public Map<String, Object> getHistory(@RequestBody StockHistoryRequest request) {
        List<StockPriceVO> data;
        if (request.getStockCode() == null || request.getStockCode().isEmpty()) {
            data = stockService.getAllHistoryPrices(request.getStartDate(), request.getEndDate());
        } else {
            data = stockService.getHistoryPrices(
                    request.getStockCode(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    /**
     * 删除指定股票在数据库中的所有缓存数据
     *
     * @param request 包含股票代码的请求参数
     * @return 删除结果
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteByStockCode(@RequestBody StockHistoryRequest request) {
        stockService.deleteByStockCode(request.getStockCode());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}
