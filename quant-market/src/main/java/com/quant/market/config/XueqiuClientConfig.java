package com.quant.market.config;

import com.quant.market.client.XueqiuScreenerClient;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class XueqiuClientConfig {

    @Resource
    private StockCodeSyncProperties stockCodeSyncProperties;

    @Bean
    public XueqiuScreenerClient xueqiuScreenerClient(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder
                .baseUrl(stockCodeSyncProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, stockCodeSyncProperties.getUserAgent())
                .defaultHeader("X-Requested-With", "XMLHttpRequest")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(XueqiuScreenerClient.class);
    }
}
