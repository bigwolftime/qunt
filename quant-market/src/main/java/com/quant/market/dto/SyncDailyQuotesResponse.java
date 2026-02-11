package com.quant.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日线同步结果。
 */
@Data
@AllArgsConstructor
public class SyncDailyQuotesResponse {

    /** 实际处理股票数。 */
    private int processedStocks;

    /** 成功股票数。 */
    private int successStocks;

    /** 失败股票数。 */
    private int failedStocks;

    /** 抓取到的日线条数。 */
    private int fetchedKlineRows;

    /** 写入条数（含更新）。 */
    private int upsertRows;

    /** 总耗时毫秒数。 */
    private long durationMs;
}
