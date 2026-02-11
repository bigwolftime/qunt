package com.quant.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日频估值同步结果。
 */
@Data
@AllArgsConstructor
public class SyncDailyValuationsResponse {

    /** 实际处理股票数。 */
    private int processedStocks;

    /** 成功股票数。 */
    private int successStocks;

    /** 失败股票数。 */
    private int failedStocks;

    /** 抓取到的估值条数。 */
    private int fetchedValuationRows;

    /** 写入条数（含更新）。 */
    private int upsertRows;

    /** 总耗时毫秒数。 */
    private long durationMs;
}
