package com.quant.dto;

public record MarketPriceResponse(
        String symbol,
        String price
) {
}
