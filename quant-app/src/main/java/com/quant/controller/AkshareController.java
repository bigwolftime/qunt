package com.quant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.quant.market.client.AkshareHttpClient;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/akshare")
public class AkshareController {

    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Resource
    private AkshareHttpClient akshareHttpClient;

    /**
     * 代理 AKShare 日线接口，便于在 Java 服务内联调。
     */
    @GetMapping("/stock-zh-a-hist")
    public JsonNode stockZhAHist(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "adjust", defaultValue = "") String adjust
    ) {
        String effectiveEndDate = endDate == null || endDate.isBlank()
                ? LocalDate.now().format(BASIC_DATE_FORMATTER)
                : endDate;
        String effectiveStartDate = startDate == null || startDate.isBlank()
                ? LocalDate.now().minusMonths(6).format(BASIC_DATE_FORMATTER)
                : startDate;
        return akshareHttpClient.stockZhAHist(symbol, "daily", effectiveStartDate, effectiveEndDate, adjust);
    }

    /**
     * 获取 A 股代码与名称列表。
     */
    @GetMapping("/stock-info-a-code-name")
    public JsonNode stockInfoACodeName() {
        return akshareHttpClient.stockInfoACodeName();
    }
}
