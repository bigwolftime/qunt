package com.quant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查看 AKShare 股票列表接口返回字段分布。
 */
class StockListFieldsTest {

    private static final Logger log = LoggerFactory.getLogger(StockListFieldsTest.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AKSHARE_BASE_URL = System.getenv().getOrDefault("AKSHARE_BASE_URL", "http://localhost:18080");

    @Test
    void shouldPrintStockListFieldsFromAkshare() throws Exception {
        JsonNode array = fetchStockList();
        assertThat(array).isNotNull();
        assertThat(array.isArray()).isTrue();
        assertThat(array.isEmpty()).isFalse();

        int inspectSize = array.size();
        int sampleSize = Math.min(5, inspectSize);
        for (int i = 0; i < sampleSize; i++) {
            log.info("[stock_list_sample] idx={}, raw={}", i, array.get(i).toString());
        }

        Map<String, Integer> fieldCount = new LinkedHashMap<>();
        for (int i = 0; i < inspectSize; i++) {
            JsonNode row = array.get(i);
            row.fieldNames().forEachRemaining(field -> fieldCount.merge(field, 1, Integer::sum));
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(fieldCount.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        log.info("[stock_list_fields] totalRows={}, inspectedRows={}, distinctFields={}",
                array.size(), inspectSize, sorted.size());
        for (Map.Entry<String, Integer> entry : sorted) {
            log.info("[stock_list_fields] field={}, appearCount={}", entry.getKey(), entry.getValue());
        }
    }

    private JsonNode fetchStockList() throws Exception {
        String url = AKSHARE_BASE_URL + "/api/public/stock_info_a_code_name";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("akshare request failed, status=" + response.statusCode() + ", body=" + response.body());
        }
        return OBJECT_MAPPER.readTree(response.body());
    }
}
