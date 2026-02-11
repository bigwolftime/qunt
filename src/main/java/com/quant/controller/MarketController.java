package com.quant.controller;

import com.quant.dto.MarketPriceResponse;
import com.quant.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{symbol}")
    public MarketPriceResponse getPrice(@PathVariable String symbol) {
        return marketDataService.getPrice(symbol.toUpperCase());
    }
}
