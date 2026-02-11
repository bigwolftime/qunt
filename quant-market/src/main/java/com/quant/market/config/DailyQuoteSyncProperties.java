package com.quant.market.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日线同步配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "quant.sync.daily-quote")
public class DailyQuoteSyncProperties {

    /** 同步最近多少个月数据。 */
    private int recentMonths = 6;

    /** 每只股票抓取后的限流毫秒数。 */
    private long throttleMs = 300L;

    /** 最大重试次数。 */
    private int maxRetries = 3;

    /** 重试退避毫秒数。 */
    private long retryBackoffMs = 1200L;

    /** 单次批量入库条数。 */
    private int batchSize = 200;

    /** 复权方式: QFQ/HFQ/NONE。 */
    private String adjust = "QFQ";

    /** 最大处理股票数, 0 表示不限制。 */
    private int maxStocks = 0;

    /** 增量模式回看天数，避免漏补权或晚到数据。 */
    private int incrementalLookbackDays = 3;
}
