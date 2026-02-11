package com.quant.market.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AKShare HTTP 网关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "quant.akshare")
public class AkshareHttpProperties {

    /** AKTools 服务地址。 */
    private String baseUrl = "http://localhost:18080";

    /** 请求头 User-Agent。 */
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
}
