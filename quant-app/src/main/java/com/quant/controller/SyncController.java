package com.quant.controller;

import com.quant.market.dto.SyncDailyQuotesResponse;
import com.quant.market.dto.SyncDailyValuationsResponse;
import com.quant.market.service.StockDailyQuoteSyncService;
import com.quant.market.service.StockCodeSyncService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Resource
    private StockCodeSyncService stockCodeSyncService;

    @Resource
    private StockDailyQuoteSyncService stockDailyQuoteSyncService;

    @PostMapping("/stock-codes")
    public void syncStockCodes() {
        stockCodeSyncService.doSync();
    }

    @PostMapping("/daily-quotes")
    public SyncDailyQuotesResponse syncDailyQuotes(@RequestParam(value = "code", required = false) String code) {
        return stockDailyQuoteSyncService.syncRecentDailyQuotes(parseCodes(code));
    }

    @PostMapping("/daily-quotes/incremental")
    public SyncDailyQuotesResponse syncDailyQuotesIncremental(@RequestParam(value = "code", required = false) String code) {
        return stockDailyQuoteSyncService.syncIncrementalDailyQuotes(parseCodes(code));
    }

    @PostMapping("/daily-valuations")
    public SyncDailyValuationsResponse syncDailyValuations(@RequestParam(value = "code", required = false) String code) {
        return stockDailyQuoteSyncService.syncRecentDailyValuations(parseCodes(code));
    }

    @PostMapping("/daily-valuations/incremental")
    public SyncDailyValuationsResponse syncDailyValuationsIncremental(@RequestParam(value = "code", required = false) String code) {
        return stockDailyQuoteSyncService.syncIncrementalDailyValuations(parseCodes(code));
    }

    private List<String> parseCodes(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        return Arrays.stream(code.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
