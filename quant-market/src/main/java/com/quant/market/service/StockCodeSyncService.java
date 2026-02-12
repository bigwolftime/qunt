package com.quant.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quant.enums.StockBoardTypeEnum;
import com.quant.enums.StockExchangeEnum;
import com.quant.model.entity.StockSecurityEntity;
import com.quant.persistence.mapper.StockSecurityMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StockCodeSyncService {

    @Resource
    private AkshareRetryService akshareRetryService;

    @Resource
    private StockSecurityMapper stockSecurityMapper;


    /**
     * 初始化/同步股票列表
     */
    public void doSync() {
        log.info("[stock-code-sync] start, source=akshare.stock_info_a_code_name");

        try {
            JsonNode response = akshareRetryService.stockInfoACodeName();
            if (response == null || !response.isArray()) {
                throw new IllegalStateException("akshare stock list response is not array");
            }

            List<StockSecurityEntity> rows = toRows(response);

            for (StockSecurityEntity entity : rows) {
                try {
                    stockSecurityMapper.insert(entity);
                } catch (DuplicateKeyException e) {
                    // ignore
                }
            }

            log.info("[stock-code-sync] done, source=akshare");
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

}
