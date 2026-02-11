package com.quant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票日频行情实体。
 */
@Data
@Accessors(chain = true)
@TableName("stock_daily_quote")
public class StockDailyQuoteEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 统一证券代码，如 002342.SZ。 */
    private String tsCode;

    /** 交易日。 */
    private LocalDate tradeDate;

    /** 开盘价。 */
    private BigDecimal open;

    /** 最高价。 */
    private BigDecimal high;

    /** 最低价。 */
    private BigDecimal low;

    /** 收盘价。 */
    private BigDecimal close;

    /** 昨收价。 */
    private BigDecimal preClose;

    /** 涨跌额。 */
    private BigDecimal changeAmt;

    /** 涨跌幅。 */
    private BigDecimal pctChg;

    /** 成交量(股)。 */
    private Long vol;

    /** 成交额(元)。 */
    private BigDecimal amount;

    /** 换手率。 */
    private BigDecimal turnoverRate;
}
