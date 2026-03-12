package com.quant.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.enums.TradeOrderStatusEnum;
import com.quant.model.entity.RealtimeHoldingEntity;
import com.quant.model.entity.TradeRecordEntity;
import com.quant.persistence.mapper.RealtimeHoldingMapper;
import com.quant.persistence.mapper.TradeRecordMapper;
import com.quant.trade.config.AndroidTradeProperties;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Android 券商自动化交易服务。
 */
@Service
public class AndroidTradeAutomationService {

    @Resource
    private AndroidTradeProperties androidTradeProperties;

    @Resource
    private BrokerAppDriverFactory brokerAppDriverFactory;

    @Resource
    private RealtimeHoldingMapper realtimeHoldingMapper;

    @Resource
    private TradeRecordMapper tradeRecordMapper;

    public List<RealtimeHoldingEntity> syncRealtimeHoldings(String brokerCode, String accountId) {
        assertEnabled();
        String effectiveAccountId = StringUtils.defaultIfBlank(accountId, androidTradeProperties.getDefaultAccountId());
        if (StringUtils.isBlank(effectiveAccountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        BrokerAppDriver driver = brokerAppDriverFactory.getDriver(brokerCode);
        List<RealtimeHoldingEntity> holdings = driver.captureHoldings(effectiveAccountId);
        for (List<RealtimeHoldingEntity> chunk : ListUtils.partition(holdings, 50)) {
            realtimeHoldingMapper.upsertBatch(chunk);
        }
        return holdings;
    }

    public TradeRecordEntity executeTrade(Long tradeRecordId) {
        assertEnabled();
        if (tradeRecordId == null) {
            throw new IllegalArgumentException("tradeRecordId is required");
        }
        TradeRecordEntity tradeRecord = tradeRecordMapper.selectById(tradeRecordId);
        if (tradeRecord == null) {
            throw new IllegalArgumentException("trade record not found: " + tradeRecordId);
        }
        tradeRecord.setStatus(TradeOrderStatusEnum.RUNNING.name());
        tradeRecord.setStatusMessage("executing by adb automation");
        tradeRecordMapper.updateById(tradeRecord);

        try {
            BrokerAppDriver driver = brokerAppDriverFactory.getDriver(tradeRecord.getBrokerCode());
            TradeRecordEntity executed = driver.submitOrder(tradeRecord);
            tradeRecordMapper.updateById(executed);
            return executed;
        } catch (Exception e) {
            tradeRecord.setStatus(TradeOrderStatusEnum.FAILED.name());
            tradeRecord.setStatusMessage(StringUtils.left(StringUtils.defaultString(e.getMessage()), 255));
            tradeRecord.setExecutedAt(LocalDateTime.now());
            tradeRecordMapper.updateById(tradeRecord);
            throw e;
        }
    }

    public List<TradeRecordEntity> loadPendingTrades(String brokerCode, String accountId) {
        return tradeRecordMapper.selectList(new LambdaQueryWrapper<TradeRecordEntity>()
                .eq(StringUtils.isNotBlank(brokerCode), TradeRecordEntity::getBrokerCode, brokerCode)
                .eq(StringUtils.isNotBlank(accountId), TradeRecordEntity::getAccountId, accountId)
                .in(TradeRecordEntity::getStatus, List.of(TradeOrderStatusEnum.CREATED.name(), TradeOrderStatusEnum.FAILED.name()))
                .orderByAsc(TradeRecordEntity::getRequestedAt));
    }

    private void assertEnabled() {
        if (!androidTradeProperties.isEnabled()) {
            throw new IllegalStateException("android trade automation is disabled");
        }
    }
}
