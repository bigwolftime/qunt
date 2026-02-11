package com.quant.market.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 股票代码同步配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "quant.sync.stock-code")
public class StockCodeSyncProperties {

    /** 上游接口基础地址。 */
    private String baseUrl = "https://xueqiu.com";

    /** 请求头 User-Agent。 */
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";

    /** 每页抓取数量。 */
    private int pageSize = 100;

    /** 最大抓取页数。 */
    private int maxPages = 200;

    /** 页间限流毫秒数。 */
    private long throttleMs = 400;

    /** 最大重试次数。 */
    private int maxRetries = 3;

    /** 重试退避毫秒数。 */
    private long retryBackoffMs = 1200;
}
