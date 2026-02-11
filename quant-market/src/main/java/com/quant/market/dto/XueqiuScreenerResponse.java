package com.quant.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record XueqiuScreenerResponse(
        @JsonProperty("data") Data data,
        @JsonProperty("error_code") int errorCode,
        @JsonProperty("error_description") String errorDescription
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("count") int count,
            @JsonProperty("list") List<Item> list
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("name") String name,
            @JsonProperty("exchange") String exchange,
            // 兼容源接口不同命名风格
            @JsonAlias({"indcode", "ind_code"}) @JsonProperty("indcode") String indCode,
            @JsonAlias({"areacode", "area_code"}) @JsonProperty("areacode") String areaCode
    ) {
    }
}
