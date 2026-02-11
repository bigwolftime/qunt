package com.quant.service;

import com.quant.client.BinanceMarketClient;
import com.quant.dto.MarketPriceResponse;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private final BinanceMarketClient marketClient;

    public MarketDataService(BinanceMarketClient marketClient) {
        this.marketClient = marketClient;
    }

    public MarketPriceResponse getPrice(String symbol) {
        return marketClient.getTickerPrice(symbol);
    }
}
