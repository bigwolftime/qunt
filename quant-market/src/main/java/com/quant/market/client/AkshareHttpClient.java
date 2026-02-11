package com.quant.market.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface AkshareHttpClient {

    @GetExchange("/api/public/stock_zh_a_hist")
    JsonNode stockZhAHist(
            @RequestParam("symbol") String symbol,
            @RequestParam("period") String period,
            @RequestParam("start_date") String startDate,
            @RequestParam("end_date") String endDate,
            @RequestParam("adjust") String adjust
    );

    @GetExchange("/api/public/stock_value_em")
    JsonNode stockValueEm(@RequestParam("symbol") String symbol);

    @GetExchange("/api/public/stock_info_a_code_name")
    JsonNode stockInfoACodeName();
}
