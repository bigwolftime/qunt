package com.quant.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quant.market.config.DailyQuoteSyncProperties;
import com.quant.market.dto.SyncDailyQuotesResponse;
import com.quant.market.dto.SyncDailyValuationsResponse;
import com.quant.persistence.mapper.StockDailyQuoteMapper;
import com.quant.persistence.mapper.StockDailyValuationMapper;
import com.quant.persistence.mapper.StockSecurityMapper;
import com.quant.model.entity.StockDailyQuoteEntity;
import com.quant.model.entity.StockDailyValuationEntity;
import com.quant.model.entity.StockSecurityEntity;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 日频行情与估值同步服务（AKShare HTTP 版）。
 */
@Service
public class StockDailyQuoteSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockDailyQuoteSyncService.class);
    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Resource
    private StockSecurityMapper stockSecurityMapper;

    @Resource
    private StockDailyQuoteMapper stockDailyQuoteMapper;

    @Resource
    private StockDailyValuationMapper stockDailyValuationMapper;

    @Resource
    private AkshareRetryService akshareRetryService;

    @Resource
    private DailyQuoteSyncProperties dailyQuoteSyncProperties;

    /**
     * 同步最近 N 个月日线（窗口全量 + 幂等 upsert）。
     */
    public SyncDailyQuotesResponse syncRecentDailyQuotes() {
        return syncRecentDailyQuotes(Collections.emptyList());
    }

    /**
     * 同步指定股票最近 N 个月日线（窗口全量 + 幂等 upsert）。
     */
    public SyncDailyQuotesResponse syncRecentDailyQuotes(List<String> targetCodes) {
        return doSync(false, targetCodes);
    }

    /**
     * 增量同步日线（基于已入库最大交易日续拉）。
     */
    public SyncDailyQuotesResponse syncIncrementalDailyQuotes() {
        return syncIncrementalDailyQuotes(Collections.emptyList());
    }

    /**
     * 增量同步指定股票日线（基于已入库最大交易日续拉）。
     */
    public SyncDailyQuotesResponse syncIncrementalDailyQuotes(List<String> targetCodes) {
        return doSync(true, targetCodes);
    }

    /**
     * 初始化同步最近 N 个月估值（窗口全量 + 幂等 upsert）。
     */
    public SyncDailyValuationsResponse syncRecentDailyValuations() {
        return syncRecentDailyValuations(Collections.emptyList());
    }

    /**
     * 初始化同步指定股票最近 N 个月估值（窗口全量 + 幂等 upsert）。
     */
    public SyncDailyValuationsResponse syncRecentDailyValuations(List<String> targetCodes) {
        return doValuationSync(false, targetCodes);
    }

    /**
     * 增量同步估值（基于已入库最大交易日续拉）。
     */
    public SyncDailyValuationsResponse syncIncrementalDailyValuations() {
        return syncIncrementalDailyValuations(Collections.emptyList());
    }

    /**
     * 增量同步指定股票估值（基于已入库最大交易日续拉）。
     */
    public SyncDailyValuationsResponse syncIncrementalDailyValuations(List<String> targetCodes) {
        return doValuationSync(true, targetCodes);
    }

    private SyncDailyQuotesResponse doSync(boolean incrementalMode, List<String> targetCodes) {
        LocalDate defaultBeginDate = LocalDate.now().minusMonths(dailyQuoteSyncProperties.getRecentMonths());
        LocalDate endDate = LocalDate.now();
        String end = endDate.format(BASIC_DATE_FORMATTER);
        String adjust = resolveAdjustParam();

        List<StockSecurityEntity> allStocks = stockSecurityMapper.selectList(
                new LambdaQueryWrapper<StockSecurityEntity>()
                        .select(StockSecurityEntity::getTsCode, StockSecurityEntity::getCode, StockSecurityEntity::getExchange)
        );
        List<StockSecurityEntity> stocks = selectStocksForSync(allStocks, targetCodes);

        int processedStocks = 0;
        int successStocks = 0;
        int failedStocks = 0;
        int fetchedKlineRows = 0;
        int upsertRows = 0;
        long startMs = System.currentTimeMillis();
        int batchSize = Math.max(1, dailyQuoteSyncProperties.getBatchSize());

        log.info("[daily-quote-sync] start, mode={}, source=akshare, stocks={}, targetCodes={}, defaultBegin={}, end={}, adjust={}, batchSize={}, throttleMs={}ms",
                incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                stocks.size(),
                normalizeCodes(targetCodes),
                defaultBeginDate.format(BASIC_DATE_FORMATTER),
                end,
                StringUtils.defaultIfBlank(adjust, "none"),
                batchSize,
                dailyQuoteSyncProperties.getThrottleMs());

        for (StockSecurityEntity stock : stocks) {
            processedStocks++;
            try {
                String beg = resolveBeginDate(stock.getTsCode(), incrementalMode, defaultBeginDate).format(BASIC_DATE_FORMATTER);
                JsonNode history = fetchHistoryWithRetry(stock.getCode(), beg, end, adjust);
                List<StockDailyQuoteEntity> rows = toQuoteEntities(stock.getTsCode(), history);

                if (CollectionUtils.isEmpty(rows)) {
                    successStocks++;
                    syncDailyValuationSnapshot(stock, incrementalMode);
                    log.info("[daily-quote-sync] empty kline, tsCode={}, begin={}, end={}", stock.getTsCode(), beg, end);
                    sleepQuietly(dailyQuoteSyncProperties.getThrottleMs());
                    continue;
                }

                for (List<StockDailyQuoteEntity> chunk : ListUtils.partition(rows, batchSize)) {
                    stockDailyQuoteMapper.upsertBatch(chunk);
                    upsertRows += chunk.size();
                }

                fetchedKlineRows += rows.size();
                successStocks++;
                syncDailyValuationSnapshot(stock, incrementalMode);
                log.info("[daily-quote-sync] synced, mode={}, tsCode={}, begin={}, end={}, rows={}",
                        incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                        stock.getTsCode(),
                        beg,
                        end,
                        rows.size());
                sleepQuietly(dailyQuoteSyncProperties.getThrottleMs());
            } catch (Exception ex) {
                failedStocks++;
                log.error("[daily-quote-sync] failed, mode={}, tsCode={}, message={}",
                        incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                        stock.getTsCode(),
                        ex.getMessage(),
                        ex);
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("[daily-quote-sync] done, mode={}, processedStocks={}, successStocks={}, failedStocks={}, fetchedKlineRows={}, upsertRows={}, durationMs={}",
                incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                processedStocks,
                successStocks,
                failedStocks,
                fetchedKlineRows,
                upsertRows,
                durationMs);

        return new SyncDailyQuotesResponse(processedStocks, successStocks, failedStocks, fetchedKlineRows, upsertRows, durationMs);
    }

    private SyncDailyValuationsResponse doValuationSync(boolean incrementalMode, List<String> targetCodes) {
        LocalDate defaultBeginDate = LocalDate.now().minusMonths(dailyQuoteSyncProperties.getRecentMonths());
        LocalDate endDate = LocalDate.now();

        List<StockSecurityEntity> allStocks = stockSecurityMapper.selectList(
                new LambdaQueryWrapper<StockSecurityEntity>()
                        .select(StockSecurityEntity::getTsCode, StockSecurityEntity::getCode)
        );
        List<StockSecurityEntity> stocks = selectStocksForSync(allStocks, targetCodes);

        int processedStocks = 0;
        int successStocks = 0;
        int failedStocks = 0;
        int fetchedValuationRows = 0;
        int upsertRows = 0;
        long startMs = System.currentTimeMillis();
        int batchSize = Math.max(1, dailyQuoteSyncProperties.getBatchSize());

        log.info("[daily-valuation-sync] start, mode={}, source=akshare.stock_value_em, stocks={}, targetCodes={}, defaultBegin={}, end={}, batchSize={}, throttleMs={}ms",
                incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                stocks.size(),
                normalizeCodes(targetCodes),
                defaultBeginDate,
                endDate,
                batchSize,
                dailyQuoteSyncProperties.getThrottleMs());

        for (StockSecurityEntity stock : stocks) {
            processedStocks++;
            try {
                LocalDate beginDate = resolveValuationBeginDate(stock.getTsCode(), incrementalMode, defaultBeginDate);
                JsonNode valuationArray = fetchValuationWithRetry(stock.getCode());
                List<StockDailyValuationEntity> rows = toValuationEntities(stock.getTsCode(), valuationArray, beginDate, endDate);
                fetchedValuationRows += rows.size();

                for (List<StockDailyValuationEntity> chunk : ListUtils.partition(rows, batchSize)) {
                    upsertRows += stockDailyValuationMapper.upsertBatch(chunk);
                }

                successStocks++;
                log.info("[daily-valuation-sync] synced, mode={}, tsCode={}, begin={}, end={}, rows={}",
                        incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                        stock.getTsCode(), beginDate, endDate, rows.size());
                sleepQuietly(dailyQuoteSyncProperties.getThrottleMs());
            } catch (Exception ex) {
                failedStocks++;
                log.error("[daily-valuation-sync] failed, mode={}, tsCode={}, message={}",
                        incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                        stock.getTsCode(), ex.getMessage(), ex);
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("[daily-valuation-sync] done, mode={}, processedStocks={}, successStocks={}, failedStocks={}, fetchedValuationRows={}, upsertRows={}, durationMs={}",
                incrementalMode ? "INCREMENTAL" : "WINDOW_FULL",
                processedStocks, successStocks, failedStocks, fetchedValuationRows, upsertRows, durationMs);
        return new SyncDailyValuationsResponse(processedStocks, successStocks, failedStocks, fetchedValuationRows, upsertRows, durationMs);
    }

    /**
     * 自动补齐当日估值快照。
     */
    private int syncDailyValuationSnapshot(StockSecurityEntity stock, boolean incrementalMode) {
        LocalDate valuationDate = resolveValuationDate(stock.getTsCode());
        if (incrementalMode) {
            LocalDate maxTradeDate = queryMaxValuationTradeDate(stock.getTsCode());
            if (maxTradeDate != null && !valuationDate.isAfter(maxTradeDate)) {
                return 0;
            }
        }

        JsonNode valuationArray = fetchValuationWithRetry(stock.getCode());
        JsonNode valuation = findValuationByDate(valuationArray, valuationDate);
        if (valuation == null) {
            log.info("[daily-valuation-sync] no valuation row, tsCode={}, tradeDate={}", stock.getTsCode(), valuationDate);
            return 0;
        }

        BigDecimal floatShare = parseDecimal(valuation, "流通股本");
        BigDecimal freeShare = parseDecimal(valuation, "自由流通股本");
        if (freeShare.compareTo(BigDecimal.ZERO) == 0) {
            freeShare = floatShare;
        }

        StockDailyValuationEntity row = new StockDailyValuationEntity();
        row.setTsCode(stock.getTsCode());
        row.setTradeDate(valuationDate);
        row.setTotalShare(parseDecimal(valuation, "总股本"));
        row.setFloatShare(floatShare);
        row.setFreeShare(freeShare);
        row.setTotalMv(parseDecimal(valuation, "总市值"));
        row.setCircMv(parseDecimal(valuation, "流通市值"));
        row.setPe(parseDecimal(valuation, "PE(静)", "静态市盈率", "pe"));
        row.setPeTtm(parseDecimal(valuation, "PE(TTM)", "滚动市盈率", "pe_ttm"));
        row.setPb(parseDecimal(valuation, "市净率", "pb"));
        row.setPs(parseDecimal(valuation, "市销率", "ps"));
        row.setPsTtm(parseDecimal(valuation, "市销率TTM", "市销率(TTM)", "ps_ttm"));
        row.setDvRatio(parseDecimal(valuation, "股息率", "股息率(%)", "dv_ratio"));
        row.setDvTtm(parseDecimal(valuation, "股息率TTM", "股息率(TTM)", "dv_ttm"));
        return stockDailyValuationMapper.upsertBatch(List.of(row));
    }

    /**
     * 增量模式优先从库里最大交易日回看若干天开始抓；无历史则回退到默认窗口起点。
     */
    private LocalDate resolveBeginDate(String tsCode, boolean incrementalMode, LocalDate defaultBeginDate) {
        if (!incrementalMode) {
            return defaultBeginDate;
        }

        LocalDate maxTradeDate = queryMaxQuoteTradeDate(tsCode);
        if (maxTradeDate == null) {
            return defaultBeginDate;
        }

        int lookbackDays = Math.max(0, dailyQuoteSyncProperties.getIncrementalLookbackDays());
        LocalDate beginDate = maxTradeDate.minusDays(lookbackDays);
        if (beginDate.isAfter(LocalDate.now())) {
            return LocalDate.now();
        }
        return beginDate;
    }

    private JsonNode fetchHistoryWithRetry(String code, String beg, String end, String adjust) {
        return akshareRetryService.stockZhAHist(code, beg, end, adjust);
    }

    private JsonNode fetchValuationWithRetry(String code) {
        return akshareRetryService.stockValueEm(code);
    }

    private List<StockDailyQuoteEntity> toQuoteEntities(String tsCode, JsonNode rows) {
        if (rows == null || !rows.isArray() || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockDailyQuoteEntity> entities = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            LocalDate tradeDate = parseDate(row, "日期", "date");
            if (tradeDate == null) {
                continue;
            }

            BigDecimal close = parseDecimal(row, "收盘", "close");
            BigDecimal changeAmt = parseDecimal(row, "涨跌额", "change", "change_amt");
            BigDecimal preClose = parseDecimal(row, "昨收", "pre_close");
            if (preClose.compareTo(BigDecimal.ZERO) == 0 && close.compareTo(BigDecimal.ZERO) != 0) {
                preClose = close.subtract(changeAmt);
            }

            StockDailyQuoteEntity entity = new StockDailyQuoteEntity();
            entity.setTsCode(tsCode);
            entity.setTradeDate(tradeDate);
            entity.setOpen(parseDecimal(row, "开盘", "open"));
            entity.setHigh(parseDecimal(row, "最高", "high"));
            entity.setLow(parseDecimal(row, "最低", "low"));
            entity.setClose(close);
            entity.setPreClose(preClose);
            entity.setChangeAmt(changeAmt);
            entity.setPctChg(parseDecimal(row, "涨跌幅", "pct_chg", "pctChg"));
            entity.setVol(parseLong(row, "成交量", "volume", "vol"));
            entity.setAmount(parseDecimal(row, "成交额", "amount"));
            entity.setTurnoverRate(parseDecimal(row, "换手率", "turnover_rate", "turnoverRate"));
            entities.add(entity);
        }
        return entities;
    }

    private JsonNode findValuationByDate(JsonNode valuationArray, LocalDate tradeDate) {
        if (valuationArray == null || !valuationArray.isArray() || valuationArray.isEmpty()) {
            return null;
        }

        JsonNode latestRow = null;
        LocalDate latestDate = null;
        for (JsonNode row : valuationArray) {
            LocalDate rowDate = parseDate(row, "数据日期", "日期", "date", "trade_date");
            if (rowDate == null) {
                continue;
            }
            if (tradeDate.equals(rowDate)) {
                return row;
            }
            if (rowDate.isBefore(tradeDate) && (latestDate == null || rowDate.isAfter(latestDate))) {
                latestDate = rowDate;
                latestRow = row;
            }
        }
        return latestRow;
    }

    private List<StockSecurityEntity> sliceByMaxStocks(List<StockSecurityEntity> stocks, int maxStocks) {
        if (CollectionUtils.isEmpty(stocks)) {
            return Collections.emptyList();
        }
        if (maxStocks <= 0 || stocks.size() <= maxStocks) {
            return stocks;
        }
        return stocks.subList(0, maxStocks);
    }

    private List<StockSecurityEntity> selectStocksForSync(List<StockSecurityEntity> allStocks, List<String> targetCodes) {
        Set<String> normalizedCodes = normalizeCodes(targetCodes);
        boolean hasTargetCodes = !normalizedCodes.isEmpty();
        List<StockSecurityEntity> stocks = filterStocksByCodes(allStocks, targetCodes);
        if (CollectionUtils.isEmpty(stocks)) {
            return stocks;
        }
        if (hasTargetCodes) {
            return stocks;
        }
        return sliceByMaxStocks(stocks, dailyQuoteSyncProperties.getMaxStocks());
    }

    private List<StockSecurityEntity> filterStocksByCodes(List<StockSecurityEntity> stocks, List<String> targetCodes) {
        if (CollectionUtils.isEmpty(stocks)) {
            return Collections.emptyList();
        }
        Set<String> normalizedCodes = normalizeCodes(targetCodes);
        if (normalizedCodes.isEmpty()) {
            return stocks;
        }

        List<StockSecurityEntity> matched = new ArrayList<>();
        for (StockSecurityEntity stock : stocks) {
            String normalizedCode = normalizeCode(stock.getCode());
            String normalizedTsCode = normalizeCode(stock.getTsCode());
            if (normalizedCodes.contains(normalizedCode) || normalizedCodes.contains(normalizedTsCode)) {
                matched.add(stock);
            }
        }
        return matched;
    }

    private Set<String> normalizeCodes(List<String> targetCodes) {
        if (CollectionUtils.isEmpty(targetCodes)) {
            return Collections.emptySet();
        }

        Set<String> normalized = new HashSet<>();
        for (String rawCode : targetCodes) {
            String value = normalizeCode(rawCode);
            if (StringUtils.isNotBlank(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private String normalizeCode(String rawCode) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(rawCode), Locale.ROOT);
    }

    private String resolveAdjustParam() {
        String configuredAdjust = StringUtils.upperCase(StringUtils.trimToEmpty(dailyQuoteSyncProperties.getAdjust()));
        if ("HFQ".equals(configuredAdjust)) {
            return "hfq";
        }
        if ("NONE".equals(configuredAdjust) || StringUtils.isBlank(configuredAdjust)) {
            return "";
        }
        if (!"QFQ".equals(configuredAdjust)) {
            log.warn("[daily-quote-sync] unknown adjust={}, fallback to qfq", dailyQuoteSyncProperties.getAdjust());
        }
        return "qfq";
    }

    private LocalDate resolveValuationDate(String tsCode) {
        LocalDate maxQuoteTradeDate = queryMaxQuoteTradeDate(tsCode);
        return maxQuoteTradeDate == null ? LocalDate.now() : maxQuoteTradeDate;
    }

    private LocalDate resolveValuationBeginDate(String tsCode, boolean incrementalMode, LocalDate defaultBeginDate) {
        if (!incrementalMode) {
            return defaultBeginDate;
        }

        LocalDate maxTradeDate = queryMaxValuationTradeDate(tsCode);
        if (maxTradeDate == null) {
            return defaultBeginDate;
        }

        int lookbackDays = Math.max(0, dailyQuoteSyncProperties.getIncrementalLookbackDays());
        LocalDate beginDate = maxTradeDate.minusDays(lookbackDays);
        if (beginDate.isAfter(LocalDate.now())) {
            return LocalDate.now();
        }
        return beginDate;
    }

    private List<StockDailyValuationEntity> toValuationEntities(String tsCode, JsonNode valuationArray, LocalDate beginDate, LocalDate endDate) {
        if (valuationArray == null || !valuationArray.isArray() || valuationArray.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockDailyValuationEntity> rows = new ArrayList<>();
        for (JsonNode valuation : valuationArray) {
            LocalDate tradeDate = parseDate(valuation, "数据日期", "日期", "date", "trade_date");
            if (tradeDate == null || tradeDate.isBefore(beginDate) || tradeDate.isAfter(endDate)) {
                continue;
            }

            BigDecimal floatShare = parseDecimal(valuation, "流通股本");
            BigDecimal freeShare = parseDecimal(valuation, "自由流通股本");
            if (freeShare.compareTo(BigDecimal.ZERO) == 0) {
                freeShare = floatShare;
            }

            StockDailyValuationEntity row = new StockDailyValuationEntity();
            row.setTsCode(tsCode);
            row.setTradeDate(tradeDate);
            row.setTotalShare(parseDecimal(valuation, "总股本"));
            row.setFloatShare(floatShare);
            row.setFreeShare(freeShare);
            row.setTotalMv(parseDecimal(valuation, "总市值"));
            row.setCircMv(parseDecimal(valuation, "流通市值"));
            row.setPe(parseDecimal(valuation, "PE(静)", "静态市盈率", "pe"));
            row.setPeTtm(parseDecimal(valuation, "PE(TTM)", "滚动市盈率", "pe_ttm"));
            row.setPb(parseDecimal(valuation, "市净率", "pb"));
            row.setPs(parseDecimal(valuation, "市销率", "ps"));
            row.setPsTtm(parseDecimal(valuation, "市销率TTM", "市销率(TTM)", "ps_ttm"));
            row.setDvRatio(parseDecimal(valuation, "股息率", "股息率(%)", "dv_ratio"));
            row.setDvTtm(parseDecimal(valuation, "股息率TTM", "股息率(TTM)", "dv_ttm"));
            rows.add(row);
        }
        return rows;
    }

    private LocalDate parseDate(JsonNode row, String... keys) {
        for (String key : keys) {
            String raw = StringUtils.trimToEmpty(row.path(key).asText(""));
            if (StringUtils.isBlank(raw)) {
                continue;
            }
            if (raw.length() >= 10) {
                raw = raw.substring(0, 10);
            }
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ignore) {
                // 兼容异常格式，继续尝试其他字段
            }
        }
        return null;
    }

    private BigDecimal parseDecimal(JsonNode row, String... keys) {
        for (String key : keys) {
            JsonNode node = row.get(key);
            if (node == null || node.isNull()) {
                continue;
            }
            String text = StringUtils.trimToEmpty(node.asText(""));
            if (StringUtils.isBlank(text)
                    || "-".equals(text)
                    || "--".equals(text)
                    || "null".equalsIgnoreCase(text)
                    || "None".equalsIgnoreCase(text)
                    || "nan".equalsIgnoreCase(text)
                    || "NaN".equalsIgnoreCase(text)) {
                continue;
            }
            text = StringUtils.remove(text, ',');
            text = StringUtils.removeEnd(text, "%");
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignore) {
                // 兼容异常格式，继续尝试其他字段
            }
        }
        return BigDecimal.ZERO;
    }

    private Long parseLong(JsonNode row, String... keys) {
        BigDecimal value = parseDecimal(row, keys);
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private LocalDate queryMaxQuoteTradeDate(String tsCode) {
        StockDailyQuoteEntity row = stockDailyQuoteMapper.selectOne(
                new LambdaQueryWrapper<StockDailyQuoteEntity>()
                        .select(StockDailyQuoteEntity::getTradeDate)
                        .eq(StockDailyQuoteEntity::getTsCode, tsCode)
                        .orderByDesc(StockDailyQuoteEntity::getTradeDate)
                        .last("LIMIT 1")
        );
        return row == null ? null : row.getTradeDate();
    }

    private LocalDate queryMaxValuationTradeDate(String tsCode) {
        StockDailyValuationEntity row = stockDailyValuationMapper.selectOne(
                new LambdaQueryWrapper<StockDailyValuationEntity>()
                        .select(StockDailyValuationEntity::getTradeDate)
                        .eq(StockDailyValuationEntity::getTsCode, tsCode)
                        .orderByDesc(StockDailyValuationEntity::getTradeDate)
                        .last("LIMIT 1")
        );
        return row == null ? null : row.getTradeDate();
    }

    private void sleepQuietly(long ms) {
        Uninterruptibles.sleepUninterruptibly(ms, TimeUnit.MILLISECONDS);
    }
}
