package com.quant.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quant.market.client.AkshareHttpClient;
import jakarta.annotation.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class AkshareRetryService {

    @Resource
    private AkshareHttpClient akshareHttpClient;

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "#{${quant.sync.stock-code.max-retries:3}}",
            backoff = @Backoff(delayExpression = "#{${quant.sync.stock-code.retry-backoff-ms:1200}}")
    )
    public JsonNode stockInfoACodeName() {
        return akshareHttpClient.stockInfoACodeName();
    }

    @Recover
    public JsonNode recoverStockInfoACodeName(RuntimeException ex) {
        throw new IllegalStateException("fetch stock list failed after retries", ex);
    }

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "#{${quant.sync.daily-quote.max-retries:3}}",
            backoff = @Backoff(delayExpression = "#{${quant.sync.daily-quote.retry-backoff-ms:1200}}")
    )
    public JsonNode stockZhAHist(String code, String beg, String end, String adjust) {
        return akshareHttpClient.stockZhAHist(code, "daily", beg, end, adjust);
    }

    @Recover
    public JsonNode recoverStockZhAHist(RuntimeException ex, String code, String beg, String end, String adjust) {
        throw new IllegalStateException("fetch kline failed, code=" + code, ex);
    }

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "#{${quant.sync.daily-quote.max-retries:3}}",
            backoff = @Backoff(delayExpression = "#{${quant.sync.daily-quote.retry-backoff-ms:1200}}")
    )
    public JsonNode stockValueEm(String code) {
        return akshareHttpClient.stockValueEm(code);
    }

    @Recover
    public JsonNode recoverStockValueEm(RuntimeException ex, String code) {
        throw new IllegalStateException("fetch valuation failed, code=" + code, ex);
    }
}
