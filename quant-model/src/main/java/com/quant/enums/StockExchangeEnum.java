package com.quant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 交易所枚举，统一封装前缀与标准交易所编码映射关系。
 */
@Getter
@AllArgsConstructor
public enum StockExchangeEnum {

    /** 上海证券交易所。 */
    SH("SH", "SSE"),

    /** 深圳证券交易所。 */
    SZ("SZ", "SZSE"),

    /** 北京证券交易所。 */
    BJ("BJ", "BSE"),

    /** 未识别交易所。 */
    UNKNOWN("", "");

    private final String symbolPrefix;
    private final String exchangeCode;


    public static StockExchangeEnum fromSymbolPrefix(String symbolPrefix) {
        return Arrays.stream(values())
                .filter(it -> it.symbolPrefix.equalsIgnoreCase(symbolPrefix))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public static StockExchangeEnum fromStockCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return UNKNOWN;
        }
        if (stockCode.startsWith("6")) {
            return SH;
        }
        if (stockCode.startsWith("0") || stockCode.startsWith("3")) {
            return SZ;
        }
        if (stockCode.startsWith("4") || stockCode.startsWith("8")) {
            return BJ;
        }
        return UNKNOWN;
    }
}
