package com.quant.client;

import com.quant.dto.MarketPriceResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface BinanceMarketClient {

    @GetExchange("/api/v3/ticker/price")
    MarketPriceResponse getTickerPrice(@RequestParam("symbol") String symbol);
}
