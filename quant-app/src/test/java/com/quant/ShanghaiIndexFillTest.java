package com.quant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShanghaiIndexFillTest {

    private static final Logger log = LoggerFactory.getLogger(ShanghaiIndexFillTest.class);
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 2, 10);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AKSHARE_BASE_URL = System.getenv().getOrDefault("AKSHARE_BASE_URL", "http://localhost:18080");

    @Test
    void shouldLogShanghaiIndexDataFromAkshareOn20260210() throws Exception {
        String tsCode = findIndexCode("上证指数");
        assertThat(tsCode).isEqualTo("000001.SH");
        String indexSymbol = "sh000001";

        log.info("[stock_security] tsCode={}, code={}, exchange={}, name={}",
                tsCode,
                "000001",
                "SSE",
                "上证指数");

        JsonNode quoteArray = fetchIndexDaily(indexSymbol);
        assertThat(quoteArray).isNotNull();
        assertThat(quoteArray.isArray()).isTrue();
        JsonNode quote = findIndexQuoteByDate(quoteArray, TARGET_DATE.toString());
        assertThat(quote).isNotNull();
        log.info("[stock_daily_quote] tsCode={}, tradeDate={}, open={}, close={}, high={}, low={}, vol={}, amount={}, raw={}",
                tsCode,
                TARGET_DATE,
                quote.path("open").asText(""),
                quote.path("close").asText(""),
                quote.path("high").asText(""),
                quote.path("low").asText(""),
                quote.path("volume").asText(""),
                quote.path("amount").asText(""),
                quote.toString());

        JsonNode valuationArray = fetchIndexValuationOptional("上证指数");
        JsonNode valuation = findIndexValuationByDate(valuationArray, TARGET_DATE.toString());
        if (valuation == null) {
            log.info("[stock_daily_valuation] tsCode={}, tradeDate={}, pe=N/A, peTtm=N/A, pb=N/A, reason=akshare该指数暂无估值接口数据",
                    tsCode, TARGET_DATE);
            return;
        }
        log.info("[stock_daily_valuation] tsCode={}, tradeDate={}, pe={}, peTtm={}, pb={}, raw={}",
                tsCode,
                TARGET_DATE,
                valuation.path("静态市盈率").asText(""),
                valuation.path("滚动市盈率").asText(""),
                "N/A",
                valuation.toString());
    }

    private String findIndexCode(String indexName) {
        Map<String, String> indexCodeMap = Map.of(
                "上证指数", "000001.SH"
        );
        return indexCodeMap.get(indexName);
    }

    private JsonNode fetchIndexDaily(String symbol) throws Exception {
        String url = AKSHARE_BASE_URL + "/api/public/stock_zh_index_daily_em?symbol=" + encode(symbol);
        return doGet(url);
    }

    private JsonNode fetchIndexValuationOptional(String symbol) throws Exception {
        String url = AKSHARE_BASE_URL + "/api/public/stock_index_pe_lg?symbol=" + encode(symbol);
        return doGetAllowError(url);
    }

    private JsonNode doGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("akshare request failed, status=" + response.statusCode() + ", url=" + url + ", body=" + response.body());
        }
        return OBJECT_MAPPER.readTree(response.body());
    }

    private JsonNode doGetAllowError(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            return null;
        }
        return OBJECT_MAPPER.readTree(response.body());
    }

    private JsonNode findIndexQuoteByDate(JsonNode quoteArray, String yyyyMmDd) {
        if (quoteArray == null || !quoteArray.isArray()) {
            return null;
        }
        for (JsonNode row : quoteArray) {
            String dateText = row.path("date").asText("");
            if (dateText.startsWith(yyyyMmDd)) {
                return row;
            }
        }
        return null;
    }

    private JsonNode findIndexValuationByDate(JsonNode valuationArray, String yyyyMmDd) {
        if (valuationArray == null || !valuationArray.isArray()) {
            return null;
        }
        for (JsonNode row : valuationArray) {
            String dateText = row.path("日期").asText("");
            if (dateText.startsWith(yyyyMmDd)) {
                return row;
            }
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
