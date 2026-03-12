package com.quant.trade;

import com.quant.model.entity.RealtimeHoldingEntity;
import com.quant.model.entity.TradeRecordEntity;

import java.util.List;

/**
 * 券商 APP 自动化驱动。
 */
public interface BrokerAppDriver {

    String brokerCode();

    List<RealtimeHoldingEntity> captureHoldings(String accountId);

    TradeRecordEntity submitOrder(TradeRecordEntity tradeRecord);
}
