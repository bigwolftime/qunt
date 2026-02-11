package com.quant.market.client;

import com.quant.market.dto.XueqiuScreenerResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface XueqiuScreenerClient {

    @GetExchange("/service/screener/screen")
    XueqiuScreenerResponse screen(
            @RequestParam("category") String category,
            @RequestParam("exchange") String exchange,
            @RequestParam("order_by") String orderBy,
            @RequestParam("order") String order,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("only_count") int onlyCount
    );
}
