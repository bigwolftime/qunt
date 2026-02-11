package com.quant.market.support;

import com.quant.enums.StockBoardTypeEnum;
import com.quant.enums.StockExchangeEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * 股票板块判断工具。
 */
public final class StockBoardResolver {

    private StockBoardResolver() {
    }

    /**
     * 通过交易所+代码判断是否科创板。
     * exchange 支持 SH/SZ/BJ 或 SSE/SZSE/BSE。
     */
    public static boolean isStarBoard(String exchange, String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        StockExchangeEnum exchangeEnum = resolveExchange(exchange);
        return StockBoardTypeEnum.resolve(exchangeEnum, code.trim()) == StockBoardTypeEnum.STAR;
    }

    /**
     * 通过交易所+代码判断是否创业板。
     * exchange 支持 SH/SZ/BJ 或 SSE/SZSE/BSE。
     */
    public static boolean isGemBoard(String exchange, String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        StockExchangeEnum exchangeEnum = resolveExchange(exchange);
        return StockBoardTypeEnum.resolve(exchangeEnum, code.trim()) == StockBoardTypeEnum.GEM;
    }

    /**
     * 通过股票标识判断是否科创板。
     * 支持 SH688519、688519.SH 两种格式。
     */
    public static boolean isStarBoardBySymbol(String symbol) {
        Parsed parsed = parseSymbol(symbol);
        if (parsed == null) {
            return false;
        }
        return StockBoardTypeEnum.resolve(parsed.exchangeEnum(), parsed.code()) == StockBoardTypeEnum.STAR;
    }

    /**
     * 通过股票标识判断是否创业板。
     * 支持 SZ300196、300196.SZ 两种格式。
     */
    public static boolean isGemBoardBySymbol(String symbol) {
        Parsed parsed = parseSymbol(symbol);
        if (parsed == null) {
            return false;
        }
        return StockBoardTypeEnum.resolve(parsed.exchangeEnum(), parsed.code()) == StockBoardTypeEnum.GEM;
    }

    private static StockExchangeEnum resolveExchange(String exchange) {
        if (StringUtils.isBlank(exchange)) {
            return StockExchangeEnum.UNKNOWN;
        }
        String value = exchange.trim().toUpperCase();
        return StockExchangeEnum.fromSymbolPrefix(value);
    }

    private static Parsed parseSymbol(String symbol) {
        if (StringUtils.isBlank(symbol)) {
            return null;
        }
        String value = symbol.trim().toUpperCase();

        // 形如 688519.SH
        if (value.contains(".")) {
            String[] parts = value.split("\\.");
            if (parts.length != 2 || StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
                return null;
            }
            return new Parsed(parts[0], StockExchangeEnum.fromSymbolPrefix(parts[1]));
        }

        // 形如 SH688519
        if (value.length() >= 4) {
            String prefix = value.substring(0, 2);
            String code = value.substring(2);
            if (StringUtils.isBlank(code)) {
                return null;
            }
            return new Parsed(code, StockExchangeEnum.fromSymbolPrefix(prefix));
        }
        return null;
    }

    private record Parsed(String code, StockExchangeEnum exchangeEnum) {
    }
}
