package com.quant.config;

import com.quant.client.BinanceMarketClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.client.support.RestClientAdapter;

@Configuration
public class MarketClientConfig {

    @Bean
    public BinanceMarketClient binanceMarketClient(
            RestClient.Builder restClientBuilder,
            @Value("${quant.market.base-url:https://api.binance.com}") String baseUrl
    ) {
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
        return factory.createClient(BinanceMarketClient.class);
    }
}
