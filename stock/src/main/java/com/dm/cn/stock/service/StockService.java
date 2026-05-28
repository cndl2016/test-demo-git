package com.dm.cn.stock.service;

import com.dm.cn.stock.converter.StockPriceConverter;
import com.dm.cn.stock.entity.StockEntity;
import com.dm.cn.stock.entity.StockPriceEntity;
import com.dm.cn.stock.repository.StockPriceRepository;
import com.dm.cn.stock.repository.StockRepository;
import com.dm.cn.stock.util.StockCodeUtil;
import com.dm.cn.stock.util.TradeDateUtil;
import com.dm.cn.stock.vo.StockPriceVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 股票数据业务层
 * 负责股票历史价格的查询、外部接口调用及数据持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final RestTemplate restTemplate;
    private final StockPriceRepository stockPriceRepository;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 腾讯财经历史K线数据接口地址 */
    private static final String TENCENT_KLINE_URL = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get";

    /** 腾讯实时行情接口地址 */
    private static final String TENCENT_QUOTE_URL = "http://qt.gtimg.cn/q=";

    /** 新浪A股列表接口地址模板（%d 为页码，%s 为节点：sh_a / sz_a） */
    private static final String SINA_STOCK_LIST_URL =
            "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=%d&num=100&node=%s";

    /**
     * 获取指定股票在指定时间范围内的历史价格
     * 优先从SQLite数据库查询，若缓存数据未覆盖完整日期范围，
     * 则调用腾讯接口获取缺失数据并补充入库（支持分页循环获取）
     *
     * @param stockCode 股票代码，如 600519
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @return 股票价格列表，包含从开始日期到上一个交易日的数据
     */
    public List<StockPriceVO> getHistoryPrices(String stockCode, String startDate, String endDate) {
        // 同步股票基本信息
        syncStockInfo(stockCode);

        // 如果未传入结束日期，默认使用今天上一个交易日
        if (endDate == null || endDate.isEmpty()) {
            endDate = TradeDateUtil.getLastTradeDate();
        }

        // 先从数据库查询目标范围内的数据
        List<StockPriceEntity> dbResult = stockPriceRepository
                .findByStockCodeAndDateBetweenOrderByDateAsc(stockCode, startDate, endDate);

        boolean needFetch = false;

        if (dbResult.isEmpty()) {
            // 数据库中完全无数据，需要调用接口
            needFetch = true;
            log.info("数据库未命中，调用腾讯接口获取, stockCode={}, startDate={}, endDate={}",
                    stockCode, startDate, endDate);
        } else {
            // 查询该股票在数据库中的全局最早和最晚日期
            Optional<StockPriceEntity> earliest = stockPriceRepository
                    .findFirstByStockCodeOrderByDateAsc(stockCode);
            Optional<StockPriceEntity> latest = stockPriceRepository
                    .findFirstByStockCodeOrderByDateDesc(stockCode);

            if (earliest.isPresent() && latest.isPresent()) {
                String dbMinDate = earliest.get().getDate();
                String dbMaxDate = latest.get().getDate();

                // 将 startDate 调整为当日或之后的第一个交易日（跳过周末），避免非交易日导致误判
                String adjustedStartDate = TradeDateUtil.getFirstTradeDateOnOrAfter(startDate);

                // 如果查询范围超出了数据库已缓存的日期范围，需要补充数据
                if (adjustedStartDate.compareTo(dbMinDate) < 0 || endDate.compareTo(dbMaxDate) > 0) {
                    needFetch = true;
                    log.info("数据库缓存不完整，需要补充数据, stockCode={}, startDate={}, endDate={}, dbMinDate={}, dbMaxDate={}",
                            stockCode, startDate, endDate, dbMinDate, dbMaxDate);
                } else {
                    log.info("从数据库命中缓存, stockCode={}, startDate={}, endDate={}, size={}",
                            stockCode, startDate, endDate, dbResult.size());
                }
            }
        }

        if (needFetch) {
            // 记录当前数据库中最早日期，用于判断接口是否返回了更早数据
            String dbMinDate = stockPriceRepository.findFirstByStockCodeOrderByDateAsc(stockCode)
                    .map(StockPriceEntity::getDate).orElse(null);

            // 循环调接口，处理腾讯 800 条返回上限
            String fetchEndDate = endDate;
            String lastApiMinDate = null;
            int totalCount = 0;

            while (true) {
                List<StockPriceVO> voList = fetchFromTencent(stockCode, startDate, fetchEndDate);
                if (voList.isEmpty()) {
                    break;
                }

                String apiMinDate = voList.get(0).getDate();

                // 如果接口返回的最早日期和数据库已有的最早日期相同，说明没有更早数据可补充
                if (apiMinDate.equals(dbMinDate)) {
                    log.info("接口未返回更早数据，停止循环, stockCode={}", stockCode);
                    break;
                }

                // 如果连续两次循环返回的最早日期相同，说明没有新数据，防止死循环
                if (apiMinDate.equals(lastApiMinDate)) {
                    log.info("接口返回数据无变化，停止循环, stockCode={}", stockCode);
                    break;
                }
                lastApiMinDate = apiMinDate;

                // 保存数据到数据库
                List<StockPriceEntity> entities = new ArrayList<>();
                for (StockPriceVO vo : voList) {
                    StockPriceEntity entity = StockPriceConverter.toEntity(vo, stockCode);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }
                stockPriceRepository.saveAll(entities);
                totalCount += entities.size();

                // 如果接口返回的最早日期已经 <= startDate，说明数据已完整
                if (apiMinDate.compareTo(startDate) <= 0) {
                    break;
                }

                // 继续往前获取更早的数据
                fetchEndDate = LocalDate.parse(apiMinDate).minusDays(1).toString();
            }

            log.info("数据已缓存到数据库, stockCode={}, totalCount={}", stockCode, totalCount);

            // 重新从数据库查询并返回
            dbResult = stockPriceRepository
                    .findByStockCodeAndDateBetweenOrderByDateAsc(stockCode, startDate, endDate);
        }

        return StockPriceConverter.toVoList(dbResult);
    }

    /**
     * 同步所有股票在指定日期范围内的历史价格
     * 从 stock 表读取全部股票，逐只调用 getHistoryPrices 获取并缓存数据
     *
     * @param startDate 开始日期，格式 yyyy-MM-dd
     * @param endDate   结束日期，格式 yyyy-MM-dd，为空时默认为上一个交易日
     * @return 所有股票的历史价格列表
     */
    public List<StockPriceVO> getAllHistoryPrices(String startDate, String endDate) {
        List<StockEntity> allStocks = stockRepository.findAll();
        if (allStocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockPriceVO> allResult = new ArrayList<>();
        int total = allStocks.size();
        int successCount = 0;

        for (StockEntity stock : allStocks) {
            try {
                List<StockPriceVO> voList = getHistoryPrices(stock.getStockCode(), startDate, endDate);
                allResult.addAll(voList);
                successCount++;
            } catch (Exception e) {
                log.warn("同步股票历史价格失败, stockCode={}", stock.getStockCode(), e);
            }
        }

        log.info("批量同步历史价格完成, total={}, success={}, records={}", total, successCount, allResult.size());
        return allResult;
    }

    /**
     * 根据股票代码查询股票基本信息
     *
     * @param stockCode 股票代码
     * @return 股票基本信息，不存在返回null
     */
    public StockEntity getStockInfo(String stockCode) {
        return stockRepository.findById(stockCode).orElseGet(() -> {
            syncStockInfo(stockCode);
            return stockRepository.findById(stockCode).orElse(null);
        });
    }

    /**
     * 同步股票基本信息
     * 调用腾讯接口获取股票实时行情并保存或更新到数据库
     *
     * @param stockCode 股票代码
     */
    public void syncStockInfo(String stockCode) {
        try {
            String symbol = StockCodeUtil.convertToTencentSymbol(stockCode);
            String url = TENCENT_QUOTE_URL + symbol;
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty()) {
                return;
            }

            // 腾讯接口返回 GB2312 编码，需要处理
            // 格式：v_sh600519="1~贵州茅台~600519~..."
            StockEntity stock = parseStockEntityFromQuote(response, stockCode, symbol);
            if (stock == null || stock.getStockName() == null || stock.getStockName().isEmpty()) {
                return;
            }

            stockRepository.save(stock);
            log.info("股票基本信息已同步, stockCode={}, stockName={}", stockCode, stock.getStockName());

        } catch (Exception e) {
            log.warn("同步股票基本信息失败, stockCode={}", stockCode, e);
        }
    }

    /**
     * 从腾讯实时行情返回文本中解析完整的股票实体信息
     *
     * @param response  接口返回文本
     * @param stockCode 股票代码
     * @param symbol    腾讯格式代码（sh/sz前缀）
     * @return 解析后的股票实体，解析失败返回null
     */
    private StockEntity parseStockEntityFromQuote(String response, String stockCode, String symbol) {
        int start = response.indexOf('"');
        int end = response.lastIndexOf('"');
        if (start == -1 || end == -1 || end <= start) {
            return null;
        }

        String content = response.substring(start + 1, end);
        String[] parts = content.split("~");
        if (parts.length < 2) {
            return null;
        }

        StockEntity stock = new StockEntity();
        stock.setStockCode(stockCode);
        stock.setStockName(parts[1]);
        stock.setMarket(symbol.startsWith("sh") ? "上海" : "深圳");
        populateStockFields(stock, parts);
        return stock;
    }

    private void populateStockFields(StockEntity stock, String[] parts) {
        if (parts.length > 3) {
            stock.setCurrentPrice(parseDouble(parts[3]));
        }
        if (parts.length > 4) {
            stock.setPreviousClose(parseDouble(parts[4]));
        }
        if (parts.length > 5) {
            stock.setOpenPrice(parseDouble(parts[5]));
        }
        if (parts.length > 6) {
            stock.setVolume(parseLong(parts[6]));
        }
        if (parts.length > 31) {
            stock.setChangeAmount(parseDouble(parts[31]));
        }
        if (parts.length > 32) {
            stock.setChangePercent(parseDouble(parts[32]));
        }
        if (parts.length > 33) {
            stock.setHighPrice(parseDouble(parts[33]));
        }
        if (parts.length > 34) {
            stock.setLowPrice(parseDouble(parts[34]));
        }
        if (parts.length > 38) {
            stock.setTurnover(parseDouble(parts[38]));
        }
        if (parts.length > 39) {
            stock.setPeRatio(parseDouble(parts[39]));
        }
        if (parts.length > 43) {
            stock.setAmplitude(parseDouble(parts[43]));
        }
        if (parts.length > 44) {
            stock.setFloatMarketCap(parseDouble(parts[44]));
        }
        if (parts.length > 45) {
            stock.setTotalMarketCap(parseDouble(parts[45]));
        }
        if (parts.length > 46) {
            stock.setPbRatio(parseDouble(parts[46]));
        }
        if (parts.length > 47) {
            stock.setLimitUp(parseDouble(parts[47]));
        }
        if (parts.length > 48) {
            stock.setLimitDown(parseDouble(parts[48]));
        }
    }

    /**
     * 调用腾讯财经接口获取股票历史K线数据
     *
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 解析后的股票价格列表，按日期升序排列
     */
    private List<StockPriceVO> fetchFromTencent(String stockCode, String startDate, String endDate) {
        try {
            // 将股票代码转换为腾讯接口格式（sh/sz前缀）
            String symbol = StockCodeUtil.convertToTencentSymbol(stockCode);

            // 拼接请求URL：股票代码、日线、开始日期、结束日期、前复权
            String url = TENCENT_KLINE_URL +
                    "?param=" + symbol + ",day," + startDate + "," + endDate + ",800,qfq";

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty()) {
                return Collections.emptyList();
            }

            // 解析JSON响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data").path(symbol).path("qfqday");
            if (dataNode.isMissingNode() || !dataNode.isArray()) {
                return Collections.emptyList();
            }

            LocalDate lastTrade = LocalDate.parse(endDate);
            LocalDate start = LocalDate.parse(startDate);

            List<StockPriceVO> result = new ArrayList<>();
            for (JsonNode node : dataNode) {
                if (!node.isArray() || node.size() < 6) continue;

                StockPriceVO vo = new StockPriceVO();
                // 腾讯接口返回数组格式：[日期, 开盘, 收盘, 最高, 最低, 成交量]
                vo.setDate(node.get(0).asText());
                vo.setOpen(parseDouble(node.get(1).asText()));
                vo.setClose(parseDouble(node.get(2).asText()));
                vo.setHigh(parseDouble(node.get(3).asText()));
                vo.setLow(parseDouble(node.get(4).asText()));
                vo.setVolume(parseLong(node.get(5).asText()));

                // 过滤：只保留在开始日期和上一个交易日之间的数据
                LocalDate date = LocalDate.parse(vo.getDate());
                if (!date.isBefore(start) && !date.isAfter(lastTrade)) {
                    result.add(vo);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("调用腾讯接口获取股票数据失败, stockCode={}, startDate={}, endDate={}",
                    stockCode, startDate, endDate, e);
            throw new RuntimeException("获取股票历史数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据股票代码删除数据库中所有缓存数据
     *
     * @param stockCode 股票代码
     */
    @Transactional
    public void deleteByStockCode(String stockCode) {
        stockPriceRepository.deleteByStockCode(stockCode);
        log.info("已删除股票缓存数据, stockCode={}", stockCode);
    }

    /**
     * 获取所有股票的今日实时行情
     * 从数据库读取全部股票列表，分批调用腾讯接口获取今日数据后保存到stock_price表
     *
     * @return 今日行情股票价格列表
     */
    public List<StockPriceVO> getAllTodayInfo() {
        List<StockEntity> allStocks = stockRepository.findAll();
        if (allStocks.isEmpty()) {
            return Collections.emptyList();
        }

        String date = TradeDateUtil.getLastTradeDate();
        List<StockPriceEntity> allEntities = new ArrayList<>();

        int batchSize = 400;
        int total = allStocks.size();
        for (int i = 0; i < total; i += batchSize) {
            List<StockEntity> batch = allStocks.subList(i, Math.min(i + batchSize, total));
            String symbols = batch.stream()
                    .map(s -> StockCodeUtil.convertToTencentSymbol(s.getStockCode()))
                    .collect(Collectors.joining(","));

            try {
                String response = restTemplate.getForObject(TENCENT_QUOTE_URL + symbols, String.class);
                if (response != null && !response.isEmpty()) {
                    List<StockPriceEntity> entities = parseBatchQuoteToPriceEntities(response, batch, date);
                    allEntities.addAll(entities);
                }
            } catch (Exception e) {
                log.warn("批量获取今日行情失败, batch={}, size={}", i / batchSize, batch.size(), e);
            }
        }

        if (!allEntities.isEmpty()) {
            stockPriceRepository.saveAll(allEntities);
            log.info("今日行情已保存到stock_price表, total={}, date={}", allEntities.size(), date);
        }

        return StockPriceConverter.toVoList(allEntities);
    }

    /**
     * 从腾讯批量行情接口响应中解析出今日价格实体列表
     *
     * @param response 腾讯接口返回的多行文本
     * @param batch    当前批次的股票实体列表（用于代码映射）
     * @param date     交易日期
     * @return 解析后的今日价格实体列表
     */
    private List<StockPriceEntity> parseBatchQuoteToPriceEntities(String response, List<StockEntity> batch, String date) {
        Map<String, StockEntity> symbolMap = batch.stream()
                .collect(Collectors.toMap(
                        s -> StockCodeUtil.convertToTencentSymbol(s.getStockCode()),
                        Function.identity()
                ));

        List<StockPriceEntity> entities = new ArrayList<>();
        for (String line : response.split(";")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int eq = line.indexOf('=');
            if (eq == -1 || eq + 2 >= line.length()) {
                continue;
            }

            String varName = line.substring(0, eq);
            String symbol = varName.substring(2);

            StockEntity stock = symbolMap.get(symbol);
            if (stock == null) {
                continue;
            }

            int start = line.indexOf('"', eq);
            int end = line.lastIndexOf('"');
            if (start == -1 || end == -1 || end <= start) {
                continue;
            }

            String content = line.substring(start + 1, end);
            String[] parts = content.split("~");
            if (parts.length < 6) {
                continue;
            }

            StockPriceEntity entity = new StockPriceEntity();
            entity.setStockCode(stock.getStockCode());
            entity.setDate(date);
            entity.setOpen(parseDouble(parts[5]));
            entity.setClose(parseDouble(parts[3]));
            if (parts.length > 33) {
                entity.setHigh(parseDouble(parts[33]));
            }
            if (parts.length > 34) {
                entity.setLow(parseDouble(parts[34]));
            }
            entity.setVolume(parseLong(parts[6]));
            entities.add(entity);
        }
        return entities;
    }

    /**
     * 同步所有A股股票基本信息
     * 先调用东方财富接口获取全量股票列表，再分批调用腾讯接口补全详细行情后入库
     *
     * @return 同步成功的股票数量
     */
    public int syncAllStockInfo() {
        List<StockEntity> allStocks = fetchAllAStocksFromEastmoney();
        log.info("获取到全量A股列表，共 {} 只", allStocks.size());

        int batchSize = 400;
        int total = allStocks.size();
        for (int i = 0; i < total; i += batchSize) {
            List<StockEntity> batch = allStocks.subList(i, Math.min(i + batchSize, total));
            String symbols = batch.stream()
                    .map(s -> StockCodeUtil.convertToTencentSymbol(s.getStockCode()))
                    .collect(Collectors.joining(","));

            try {
                String response = restTemplate.getForObject(TENCENT_QUOTE_URL + symbols, String.class);
                if (response != null && !response.isEmpty()) {
                    parseBatchQuoteResponse(response, batch);
                }
            } catch (Exception e) {
                log.warn("批量同步股票行情失败, batch={}, size={}", i / batchSize, batch.size(), e);
            }
        }

        stockRepository.saveAll(allStocks);
        log.info("全量股票基本信息同步完成, total={}", total);
        return total;
    }

    /**
     * 从东方财富接口获取沪深A股全量列表
     *
     * @return 股票基本信息列表（仅含代码和名称）
     */
    private List<StockEntity> fetchAllAStocksFromEastmoney() {
        List<StockEntity> allStocks = new ArrayList<>();
        allStocks.addAll(fetchStocksByNode("sh_a"));
        allStocks.addAll(fetchStocksByNode("sz_a"));
        return allStocks;
    }

    private List<StockEntity> fetchStocksByNode(String node) {
        List<StockEntity> stocks = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = String.format(SINA_STOCK_LIST_URL, page, node);
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isEmpty() || "[]".equals(response.trim())) {
                break;
            }

            try {
                JsonNode array = objectMapper.readTree(response);
                if (!array.isArray() || array.size() == 0) {
                    break;
                }

                for (JsonNode item : array) {
                    String code = item.path("code").asText();
                    String name = item.path("name").asText();
                    String symbol = item.path("symbol").asText();
                    if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
                        continue;
                    }

                    StockEntity stock = new StockEntity();
                    stock.setStockCode(code);
                    stock.setStockName(name);
                    stock.setMarket(symbol.startsWith("sh") ? "上海" : "深圳");
                    stocks.add(stock);
                }

                if (array.size() < 100) {
                    break;
                }
                page++;
            } catch (Exception e) {
                throw new RuntimeException("获取股票列表失败: " + e.getMessage(), e);
            }
        }

        return stocks;
    }

    /**
     * 解析腾讯批量行情接口响应，并填充到对应的股票实体中
     *
     * @param response 腾讯接口返回的多行文本
     * @param batch    当前批次的股票实体列表
     */
    private void parseBatchQuoteResponse(String response, List<StockEntity> batch) {
        Map<String, StockEntity> symbolMap = batch.stream()
                .collect(Collectors.toMap(
                        s -> StockCodeUtil.convertToTencentSymbol(s.getStockCode()),
                        Function.identity()
                ));

        for (String line : response.split(";")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            int eq = line.indexOf('=');
            if (eq == -1 || eq + 2 >= line.length()) {
                continue;
            }

            String varName = line.substring(0, eq);
            String symbol = varName.substring(2);

            StockEntity stock = symbolMap.get(symbol);
            if (stock == null) {
                continue;
            }

            int start = line.indexOf('"', eq);
            int end = line.lastIndexOf('"');
            if (start == -1 || end == -1 || end <= start) {
                continue;
            }

            String content = line.substring(start + 1, end);
            String[] parts = content.split("~");
            if (parts.length > 1) {
                stock.setStockName(parts[1]);
            }
            populateStockFields(stock, parts);
        }
    }

    /**
     * 将字符串解析为Double，处理空值、横杠及非数字字符
     */
    private Double parseDouble(String s) {
        if (s == null || s.isEmpty() || "-".equals(s)) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将字符串解析为Long，处理带小数点的数值（如成交量"18300.000"）及非数字字符
     */
    private Long parseLong(String s) {
        if (s == null || s.isEmpty() || "-".equals(s)) {
            return null;
        }
        try {
            return (long) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
