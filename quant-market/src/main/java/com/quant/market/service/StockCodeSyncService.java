package com.quant.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quant.enums.StockBoardTypeEnum;
import com.quant.enums.StockExchangeEnum;
import com.quant.market.config.StockCodeSyncProperties;
import com.quant.model.entity.StockSecurityEntity;
import com.quant.persistence.mapper.StockSecurityMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StockCodeSyncService {

    // 避免 IN 过长，按批次查询已存在代码
    private static final int IN_QUERY_BATCH_SIZE = 500;

    @Resource
    private AkshareRetryService akshareRetryService;

    @Resource
    private StockSecurityMapper stockSecurityMapper;

    @Resource
    private StockCodeSyncProperties stockCodeSyncProperties;


    /**
     * 初始化/同步股票列表
     */
    public void doSync() {
        LocalDateTime start = LocalDateTime.now();
        int inserted;

        log.info("[stock-code-sync] start, source=akshare.stock_info_a_code_name, throttleMs={}ms", stockCodeSyncProperties.getThrottleMs());

        try {
            JsonNode response = akshareRetryService.stockInfoACodeName();
            if (response == null || !response.isArray()) {
                throw new IllegalStateException("akshare stock list response is not array");
            }

            List<StockSecurityEntity> rows = toRows(response);
            inserted = batchInsertNewCodes(rows);

            // 保留固定限流，避免后续在同一任务内串行调用时触发上游限频
            Uninterruptibles.sleepUninterruptibly(stockCodeSyncProperties.getThrottleMs(), TimeUnit.MILLISECONDS);

            LocalDateTime end = LocalDateTime.now();
            long durationMs = ChronoUnit.MILLIS.between(start, end);
            log.info("[stock-code-sync] done, source=akshare, inserted={}, durationMs={}", inserted, durationMs);
        } catch (Exception e) {
            log.error("[stock-code-sync] failed, source=akshare", e);
            throw e;
        }
    }

    private List<StockSecurityEntity> toRows(JsonNode items) {
        List<StockSecurityEntity> rows = new ArrayList<>(items.size());
        for (JsonNode item : items) {
            String code = StringUtils.trimToEmpty(item.path("code").asText(StringUtils.EMPTY));
            String name = StringUtils.trimToEmpty(item.path("name").asText(StringUtils.EMPTY));
            if (StringUtils.isBlank(code)) {
                continue;
            }

            StockExchangeEnum exchangeEnum = StockExchangeEnum.fromStockCode(code);
            if (exchangeEnum == StockExchangeEnum.UNKNOWN) {
                log.warn("[stock-code-sync] skip unknown exchange by code={}", code);
                continue;
            }

            String boardType = StockBoardTypeEnum.resolve(exchangeEnum, code).getCode();

            StockSecurityEntity entity = new StockSecurityEntity();
            entity.setTsCode(code + "." + exchangeEnum.getSymbolPrefix());
            entity.setCode(code);
            entity.setExchange(exchangeEnum.getExchangeCode());
            entity.setName(name);
            entity.setArea(StringUtils.EMPTY);
            entity.setBoardType(boardType);
            rows.add(entity);
        }
        return rows;
    }

    private int batchInsertNewCodes(List<StockSecurityEntity> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        Set<String> existingTsCodes = new HashSet<>();
        // 先查库里已存在 ts_code，避免逐条查
        for (int from = 0; from < rows.size(); from += IN_QUERY_BATCH_SIZE) {
            int to = Math.min(from + IN_QUERY_BATCH_SIZE, rows.size());
            List<StockSecurityEntity> chunk = rows.subList(from, to);
            List<String> tsCodeChunk = chunk.stream().map(StockSecurityEntity::getTsCode).toList();
            List<StockSecurityEntity> existed = stockSecurityMapper.selectList(
                    new LambdaQueryWrapper<StockSecurityEntity>()
                            .select(StockSecurityEntity::getTsCode)
                            .in(StockSecurityEntity::getTsCode, tsCodeChunk)
            );
            for (StockSecurityEntity entity : existed) {
                existingTsCodes.add(entity.getTsCode());
            }
        }

        int inserted = 0;
        Set<String> seenInBatch = new HashSet<>();
        // 仅插入“库里不存在 + 本批未重复”的记录
        for (StockSecurityEntity entity : rows) {
            if (existingTsCodes.contains(entity.getTsCode()) || !seenInBatch.add(entity.getTsCode())) {
                continue;
            }

            if (stockSecurityMapper.insert(entity) > 0) {
                inserted++;
            }
        }

        return inserted;
    }

}
