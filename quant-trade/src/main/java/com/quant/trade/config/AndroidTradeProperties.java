package com.quant.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Android 券商自动化交易配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "quant.trade.android")
public class AndroidTradeProperties {

    private boolean enabled = false;

    private String adbPath = "adb";

    private String deviceSerial = "";

    private long commandTimeoutMs = 15000L;

    private long tapSettleMs = 800L;

    private String defaultBroker = "";

    private String defaultAccountId = "";

    private Map<String, BrokerUiProfileProperties> brokers = new LinkedHashMap<>();

    @Data
    public static class BrokerUiProfileProperties {

        private String packageName = "";

        private String launchActivity = "";

        private String holdingsTabText = "持仓";

        private String tradeTabText = "交易";

        private String buyTabText = "买入";

        private String sellTabText = "卖出";

        private String codeFieldResourceId = "";

        private String priceFieldResourceId = "";

        private String quantityFieldResourceId = "";

        private String codeFieldText = "";

        private String priceFieldText = "";

        private String quantityFieldText = "";

        private String submitButtonText = "";

        private String confirmButtonText = "";

        private String holdingsRowRegex = "";

        private String brokerOrderNoRegex = "";
    }
}
