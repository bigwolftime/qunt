package com.quant.market.config;

import com.quant.market.client.AkshareHttpClient;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AkshareHttpClientConfig {

    @Resource
    private AkshareHttpProperties akshareHttpProperties;

    @Bean
    public AkshareHttpClient akshareHttpClient(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder
                .baseUrl(akshareHttpProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, akshareHttpProperties.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(AkshareHttpClient.class);
    }
}
