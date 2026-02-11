package com.quant.enums;

/**
 * 股票板块类型枚举。
 */
public enum StockBoardTypeEnum {

    /** 主板。 */
    MAIN("MAIN"),

    /** 科创板。 */
    STAR("STAR"),

    /** 创业板。 */
    GEM("GEM"),

    /** 北交所。 */
    BSE("BSE"),

    /** 其他或未知。 */
    OTHER("OTHER");

    private static final String STAR_PREFIX = "688";
    private static final String GEM_PREFIX_1 = "300";
    private static final String GEM_PREFIX_2 = "301";

    private final String code;

    StockBoardTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据交易所与代码规则识别板块。
     */
    public static StockBoardTypeEnum resolve(StockExchangeEnum exchangeEnum, String stockCode) {
        if (exchangeEnum == StockExchangeEnum.SH && stockCode.startsWith(STAR_PREFIX)) {
            return STAR;
        }
        if (exchangeEnum == StockExchangeEnum.SZ
                && (stockCode.startsWith(GEM_PREFIX_1) || stockCode.startsWith(GEM_PREFIX_2))) {
            return GEM;
        }
        if (exchangeEnum == StockExchangeEnum.BJ) {
            return BSE;
        }
        if (exchangeEnum == StockExchangeEnum.SH || exchangeEnum == StockExchangeEnum.SZ) {
            return MAIN;
        }
        return OTHER;
    }
}
