## 项目概述

stock 模块是一个股票历史价格查询服务，基于 Spring Boot 3.4.0 构建，使用 SQLite 作为本地缓存数据库。核心逻辑是优先从数据库读取数据，缓存缺失时再调用外部财经接口（腾讯、新浪、东方财富）获取并持久化，以减少重复的外部调用。

主要功能包括：
- 股票历史价格查询（支持单只股票或批量全量）
- 股票基本信息查询与同步
- 全量 A 股基本信息同步
- 所有股票今日实时行情获取
- 缓存删除（按股票代码清理历史价格缓存）

## 目录结构

```
stock/
├── pom.xml
├── README.md
├── API.md
├── CLAUDE.md                          # 本文件
├── stock.db                           # SQLite 数据库文件（运行时生成）
└── src/main/
    ├── resources/application.yml       # 服务端口、数据源、JPA 配置
    └── java/com/dm/cn/
        ├── StockApplication.java       # 启动类
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