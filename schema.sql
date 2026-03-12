CREATE DATABASE IF NOT EXISTS quant
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci
  COMMENT = '量化数据库';

USE quant;

CREATE TABLE IF NOT EXISTS stock_security (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  ts_code           VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '股票唯一代码(如600000.SH)',
  code              VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '证券代码(如600000)',
  exchange          VARCHAR(8)   NOT NULL DEFAULT '' COMMENT '交易所(SSE/SZSE/BSE)',
  name              VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '证券简称',
  board_type        VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '板块类型(MAIN/STAR/GEM/BSE/OTHER)',
  create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_stock_security_ts_code (ts_code),
  KEY idx_stock_security_code_exchange (code, exchange)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票基础信息表';

CREATE TABLE IF NOT EXISTS stock_daily_quote (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  ts_code           VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '股票唯一代码',
  trade_date        DATE          NOT NULL DEFAULT '1970-01-01' COMMENT '交易日期',
  open              DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '开盘价',
  high              DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '最高价',
  low               DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '最低价',
  close             DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '收盘价',
  pre_close         DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '昨收价',
  change_amt        DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '涨跌额',
  pct_chg           DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '涨跌幅(%)',
  vol               BIGINT        NOT NULL DEFAULT 0 COMMENT '成交量(股)',
  amount            DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '成交额(元)',
  turnover_rate     DECIMAL(10,4) NOT NULL DEFAULT 0.0000 COMMENT '换手率(%)',
  create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_daily_quote_ts_code_trade_date (ts_code, trade_date),
  KEY idx_daily_quote_trade_date (trade_date),
  KEY idx_daily_quote_code_date_desc (ts_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票日线行情表';

CREATE TABLE IF NOT EXISTS stock_daily_valuation (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  ts_code           VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '股票唯一代码',
  trade_date        DATE          NOT NULL DEFAULT '1970-01-01' COMMENT '交易日期',
  total_share       DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '总股本',
  float_share       DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '流通股本',
  free_share        DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '自由流通股本',
  total_mv          DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '总市值(元)',
  circ_mv           DECIMAL(20,2) NOT NULL DEFAULT 0.00 COMMENT '流通市值(元)',
  pe                DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '市盈率',
  pe_ttm            DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '滚动市盈率TTM',
  pb                DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '市净率',
  ps                DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '市销率',
  ps_ttm            DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '滚动市销率TTM',
  dv_ratio          DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '股息率',
  dv_ttm            DECIMAL(12,4) NOT NULL DEFAULT 0.0000 COMMENT '滚动股息率TTM',
  create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_daily_valuation_ts_code_trade_date (ts_code, trade_date),
  KEY idx_daily_valuation_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票日频估值表';

CREATE TABLE IF NOT EXISTS realtime_holding (
  id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  account_id         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '账户标识',
  broker_code        VARCHAR(32)    NOT NULL DEFAULT '' COMMENT '券商编码',
  ts_code            VARCHAR(16)    NOT NULL DEFAULT '' COMMENT '股票唯一代码',
  stock_code         VARCHAR(8)     NOT NULL DEFAULT '' COMMENT '证券代码',
  stock_name         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '证券简称',
  quantity           BIGINT         NOT NULL DEFAULT 0 COMMENT '当前持仓数量',
  available_quantity BIGINT         NOT NULL DEFAULT 0 COMMENT '可用数量',
  cost_price         DECIMAL(12,4)  NOT NULL DEFAULT 0.0000 COMMENT '成本价',
  last_price         DECIMAL(12,4)  NOT NULL DEFAULT 0.0000 COMMENT '最新价',
  market_value       DECIMAL(20,2)  NOT NULL DEFAULT 0.00 COMMENT '持仓市值',
  profit_amount      DECIMAL(20,2)  NOT NULL DEFAULT 0.00 COMMENT '浮动盈亏',
  profit_ratio       DECIMAL(10,4)  NOT NULL DEFAULT 0.0000 COMMENT '浮动盈亏比(%)',
  snapshot_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  raw_payload        TEXT           NULL COMMENT '原始持仓载荷',
  create_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_realtime_holding_account_broker_ts_code (account_id, broker_code, ts_code),
  KEY idx_realtime_holding_snapshot_time (snapshot_time),
  KEY idx_realtime_holding_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时持仓表';

CREATE TABLE IF NOT EXISTS trade_record (
  id                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  account_id         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '账户标识',
  broker_code        VARCHAR(32)    NOT NULL DEFAULT '' COMMENT '券商编码',
  ts_code            VARCHAR(16)    NOT NULL DEFAULT '' COMMENT '股票唯一代码',
  stock_code         VARCHAR(8)     NOT NULL DEFAULT '' COMMENT '证券代码',
  stock_name         VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '证券简称',
  side               VARCHAR(8)     NOT NULL DEFAULT '' COMMENT '方向 BUY/SELL',
  limit_price        DECIMAL(12,4)  NOT NULL DEFAULT 0.0000 COMMENT '委托价格',
  requested_quantity BIGINT         NOT NULL DEFAULT 0 COMMENT '委托数量',
  executed_price     DECIMAL(12,4)  NOT NULL DEFAULT 0.0000 COMMENT '成交价格',
  executed_quantity  BIGINT         NOT NULL DEFAULT 0 COMMENT '成交数量',
  broker_order_no    VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '券商订单号',
  status             VARCHAR(16)    NOT NULL DEFAULT '' COMMENT '状态 CREATED/RUNNING/SUCCESS/FAILED/CANCELLED',
  status_message     VARCHAR(255)   NOT NULL DEFAULT '' COMMENT '状态说明',
  raw_payload        TEXT           NULL COMMENT '原始执行载荷',
  requested_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
  executed_at        DATETIME       NULL COMMENT '执行完成时间',
  create_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_trade_record_account_status (account_id, status),
  KEY idx_trade_record_requested_at (requested_at),
  KEY idx_trade_record_stock_code (stock_code),
  KEY idx_trade_record_broker_order_no (broker_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';
