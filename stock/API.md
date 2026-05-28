# 接口文档

本文档描述 stock 模块对外提供的所有 RESTful 接口。

**基础地址**：`http://localhost:7790`

**公共请求头**：

| 字段名 | 值 |
|--------|-----|
| Content-Type | `application/json` |

---

## 1. 查询股票历史价格

- **URL**：`POST /api/stock/history`

### 请求参数

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stockCode | String | 否 | 股票代码，如 `600519`、`000001`；为空时同步 stock 表中所有股票的历史价格 |
| startDate | String | 是 | 查询开始日期，格式 `yyyy-MM-dd`，如 `2025-05-01` |
| endDate | String | 否 | 查询结束日期，格式 `yyyy-MM-dd`，为空时默认为上一个交易日 |

### 请求示例

```json
{
  "stockCode": "600519",
  "startDate": "2025-05-01",
  "endDate": "2025-05-20"
}
```

不传 `endDate` 时：

```json
{
  "stockCode": "600519",
  "startDate": "2025-05-01"
}
```

同步所有股票历史价格（`stockCode` 为空）：

```json
{
  "stockCode": "",
  "startDate": "2025-05-01",
  "endDate": "2025-05-20"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "stockCode": "600519",
      "date": "2025-05-06",
      "open": 1507.443,
      "close": 1498.643,
      "high": 1507.443,
      "low": 1492.773,
      "volume": 18300,
      "amount": null,
      "changeAmount": null,
      "changePercent": null
    }
  ]
}
```

### 返回字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，200 表示成功 |
| message | String | 提示信息 |
| data | Array | 股票价格列表，按日期升序排列 |
| data[].stockCode | String | 股票代码 |
| data[].date | String | 交易日期 `yyyy-MM-dd` |
| data[].open | Double | 开盘价 |
| data[].close | Double | 收盘价 |
| data[].high | Double | 最高价 |
| data[].low | Double | 最低价 |
| data[].volume | Long | 成交量（股） |
| data[].amount | Double | 成交额（元），腾讯接口不返回，固定为 null |
| data[].changeAmount | Double | 涨跌额，腾讯接口不返回，固定为 null |
| data[].changePercent | Double | 涨跌幅（%），腾讯接口不返回，固定为 null |

---

## 2. 查询股票基本信息

根据股票代码查询该股票的基本信息（名称、市场、实时价格、涨跌幅、市盈率、市值等）。若数据库中无该股票记录，会自动调用腾讯接口同步后再返回。

- **URL**：`GET /api/stock/info/{stockCode}`

### 请求参数

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stockCode | String | 是 | 股票代码，如 `600519`、`000001` |

### 请求示例

```http
GET /api/stock/info/600519
```

### 返回示例（成功）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "market": "上海",
    "currentPrice": 1500.00,
    "previousClose": 1492.00,
    "openPrice": 1507.00,
    "highPrice": 1507.00,
    "lowPrice": 1492.00,
    "volume": 18300,
    "changeAmount": 8.00,
    "changePercent": 0.54,
    "turnover": 0.12,
    "peRatio": 28.5,
    "pbRatio": 8.2,
    "amplitude": 1.01,
    "totalMarketCap": 18850000.0,
    "floatMarketCap": 18850000.0,
    "limitUp": 1641.20,
    "limitDown": 1342.80
  }
}
```

### 返回示例（股票不存在）

```json
{
  "code": 404,
  "message": "股票不存在"
}
```

### 返回字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，200 表示成功，404 表示未找到 |
| message | String | 提示信息 |
| data | Object | 股票基本信息对象 |
| data.stockCode | String | 股票代码 |
| data.stockName | String | 股票名称 |
| data.market | String | 所属市场（上海 / 深圳） |
| data.currentPrice | Double | 当前价格（最新价） |
| data.previousClose | Double | 昨日收盘价 |
| data.openPrice | Double | 今日开盘价 |
| data.highPrice | Double | 今日最高价 |
| data.lowPrice | Double | 今日最低价 |
| data.volume | Long | 成交量（手） |
| data.changeAmount | Double | 涨跌额 |
| data.changePercent | Double | 涨跌幅（%） |
| data.turnover | Double | 换手率（%） |
| data.peRatio | Double | 市盈率（PE） |
| data.pbRatio | Double | 市净率（PB） |
| data.amplitude | Double | 振幅（%） |
| data.totalMarketCap | Double | 总市值（万元） |
| data.floatMarketCap | Double | 流通市值（万元） |
| data.limitUp | Double | 涨停价 |
| data.limitDown | Double | 跌停价 |

---

## 3. 同步所有A股股票信息

从新浪接口分页获取全量沪深A股列表（约5000+只），再分批调用腾讯接口补全每只股票的详细行情（当前价、涨跌幅、市盈率、市值等），最后批量写入数据库。此接口为同步执行，耗时约30~60秒，视网络情况而定。

- **URL**：`GET /api/stock/info/all`

### 请求参数

无

### 请求示例

```http
GET /api/stock/info/all
```

### 返回示例

```json
{
  "code": 200,
  "message": "同步完成",
  "data": {
    "count": 5342
  }
}
```

### 返回字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，200 表示成功 |
| message | String | 提示信息 |
| data.count | Integer | 本次同步的股票总数量 |

---

## 4. 删除股票缓存数据

根据股票代码删除 SQLite 数据库中 `stock_price` 表内该股票的所有历史价格缓存记录，下次查询时将重新调用外部接口获取。`stock` 表中的基本信息不受影响。

- **URL**：`POST /api/stock/delete`

### 请求参数

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| stockCode | String | 是 | 股票代码，如 `600519` |

### 请求示例

```json
{
  "stockCode": "600519"
}
```

### 返回示例

```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 5. 获取所有股票今日实时行情

从数据库读取全部股票列表，分批调用腾讯接口获取今日实时行情并更新数据库后返回。

- **URL**：`GET /api/stock/today`

### 请求参数

无

### 请求示例

```http
GET /api/stock/today
```

### 返回示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "stockCode": "688981",
      "date": "2026-05-21",
      "open": 129.33,
      "close": 131.33,
      "high": 132.18,
      "low": 126.5,
      "volume": 119947788,
      "amount": null,
      "changeAmount": null,
      "changePercent": null
    }
  ]
}
```

### 返回字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，200 表示成功 |
| message | String | 提示信息 |
| data | Array | 今日股票价格列表 |
| data[].stockCode | String | 股票代码 |
| data[].date | String | 交易日期 `yyyy-MM-dd` |
| data[].open | Double | 开盘价 |
| data[].close | Double | 收盘价（最新价） |
| data[].high | Double | 最高价 |
| data[].low | Double | 最低价 |
| data[].volume | Long | 成交量（股） |
| data[].amount | Double | 成交额，固定为 null |
| data[].changeAmount | Double | 涨跌额，固定为 null |
| data[].changePercent | Double | 涨跌幅（%），固定为 null |

**注意**：获取到的数据会同时写入 `stock_price` 表缓存，日期为上一个交易日。
