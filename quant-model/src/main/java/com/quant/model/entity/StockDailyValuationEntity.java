package com.quant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票日频估值实体。
 */
@Data
@Accessors(chain = true)
@TableName("stock_daily_valuation")
public class StockDailyValuationEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 统一证券代码，如 002342.SZ。 */
    private String tsCode;

    /** 交易日。 */
    private LocalDate tradeDate;

    /** 总股本。 */
    private BigDecimal totalShare;

    /** 流通股本。 */
    private BigDecimal floatShare;

    /** 自由流通股本。 */
    private BigDecimal freeShare;

    /** 总市值。 */
    private BigDecimal totalMv;

    /** 流通市值。 */
    private BigDecimal circMv;

    /** 市盈率。 */
    private BigDecimal pe;

    /** 滚动市盈率 TTM。 */
    private BigDecimal peTtm;

    /** 市净率。 */
    private BigDecimal pb;

    /** 市销率。 */
    private BigDecimal ps;

    /** 滚动市销率 TTM。 */
    private BigDecimal psTtm;

    /** 股息率。 */
    private BigDecimal dvRatio;

    /** 滚动股息率 TTM。 */
    private BigDecimal dvTtm;
}
