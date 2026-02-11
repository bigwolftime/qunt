package com.quant.market.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 东财个股快照接口配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "quant.sync.eastmoney")
public class EastMoneyQuoteProperties {

    /** 东财推送接口地址。 */
    private String baseUrl = "https://push2.eastmoney.com";

    /** 请求头 User-Agent。 */
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
}
