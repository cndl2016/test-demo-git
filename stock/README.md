# stock 模块

股票历史价格查询服务，基于 Spring Boot 构建，支持 SQLite 本地缓存，优先从数据库读取以减少外部接口调用。

## 技术栈

- Spring Boot 3.4.0
- Spring Data JPA
- SQLite
- RestTemplate
- Lombok

## 项目结构

```
stock/
├── pom.xml
├── README.md
├── API.md                                 # 接口文档
├── stock.db                               # SQLite 数据库文件（运行时生成）
└── src/main/
    ├── resources/application.yml           # 服务端口、数据源、JPA 配置
    └── java/com/dm/cn/
        ├── StockApplication.java           # 启动类
        └── stock/
            ├── config/
            │   └── RestTemplateConfig.java     # HTTP 客户端配置
            ├── controller/
            │   └── StockController.java        # REST 接口层
            ├── service/
            │   └── StockService.java           # 业务逻辑层
            ├── entity/
            │   ├── StockEntity.java            # 股票基本信息数据库实体
            │   ├── StockPriceEntity.java       # 股票价格数据库实体
            │   └── StockPriceId.java           # 复合主键类
            ├── repository/
            │   ├── StockPriceRepository.java   # 股票价格数据访问层
            │   └── StockRepository.java        # 股票基本信息数据访问层
            ├── converter/
            │   └── StockPriceConverter.java    # Entity / VO 转换器
            ├── util/
            │   ├── StockCodeUtil.java          # 股票代码转换工具
            │   └── TradeDateUtil.java          # 交易日计算工具
            └── vo/
                ├── StockPriceVO.java           # 股票价格响应对象
                └── StockHistoryRequest.java    # 历史数据查询请求对象
```

## 功能说明

### 1. 股票价格查询

接收股票代码、开始日期和可选的结束日期，返回指定日期范围内的所有日线价格数据。`stockCode` 为空时，会批量查询数据库中所有股票的历史价格。结束日期为空时，默认使用**今天上一个交易日**。

**缓存策略**：
- 首次查询：先检查 SQLite 数据库，无数据则调用腾讯财经接口获取，并缓存入库。
- 后续查询：直接从数据库返回，不再调用外部接口。

### 2. 股票基本信息查询与同步

每次查询股票历史价格时，系统自动调用腾讯实时行情接口同步该股票的基本信息（名称、市场、当前价、市盈率、市值等），存入 `stock` 表。同时提供独立的查询接口，可根据股票代码获取最新同步的基本信息。

### 3. 获取所有股票今日实时行情

从数据库读取全部股票列表，分批调用腾讯接口获取今日实时行情并更新数据库后返回，数据同时写入 `stock_price` 表缓存。

### 4. 缓存删除

根据股票代码删除 SQLite 中该股票的所有历史价格缓存记录，下次查询将重新调用外部接口。注意：此操作仅删除 `stock_price` 表数据，不影响 `stock` 表中的基本信息。

### 5. 交易日计算

自动跳过周六、周日，将今天上一个交易日作为查询的默认结束日期。注意：当前未处理法定节假日。

## 启动方式

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:7790`。

## 数据库配置

SQLite 数据库文件位置在 `application.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:sqlite:D:/project/test-demo/stock/stock.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.community.dialect.SQLiteDialect
```

- `ddl-auto: update`：启动时自动创建或更新表结构。
- 数据库文件在首次启动并写入数据后生成。

## 接口文档

详细的接口定义、请求参数和返回值说明请参见 [API.md](./API.md)。
