package com.quant.trade;

import com.quant.trade.config.AndroidTradeProperties;
import com.quant.trade.support.AdbCommandExecutor;
import com.quant.trade.support.AndroidUiNodeLocator;
import com.quant.trade.support.UiHierarchyParser;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 券商驱动工厂。
 */
@Component
public class BrokerAppDriverFactory {

    @Resource
    private AndroidTradeProperties androidTradeProperties;

    @Resource
    private AdbCommandExecutor adbCommandExecutor;

    @Resource
    private UiHierarchyParser uiHierarchyParser;

    @Resource
    private AndroidUiNodeLocator androidUiNodeLocator;

    public BrokerAppDriver getDriver(String brokerCode) {
        String effectiveBrokerCode = StringUtils.defaultIfBlank(brokerCode, androidTradeProperties.getDefaultBroker());
        if (StringUtils.isBlank(effectiveBrokerCode)) {
            throw new IllegalArgumentException("brokerCode is required");
        }
        AndroidTradeProperties.BrokerUiProfileProperties profile = androidTradeProperties.getBrokers().get(effectiveBrokerCode);
        if (profile == null) {
            throw new IllegalStateException("missing broker profile for brokerCode=" + effectiveBrokerCode);
        }
        return new ConfiguredBrokerAppDriver(
                effectiveBrokerCode,
                androidTradeProperties,
                profile,
                adbCommandExecutor,
                uiHierarchyParser,
                androidUiNodeLocator
        );
    }
}
