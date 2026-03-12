package com.quant.trade;

import com.quant.enums.StockExchangeEnum;
import com.quant.enums.TradeOrderStatusEnum;
import com.quant.model.entity.RealtimeHoldingEntity;
import com.quant.model.entity.TradeRecordEntity;
import com.quant.trade.config.AndroidTradeProperties;
import com.quant.trade.dto.UiNode;
import com.quant.trade.support.AdbCommandExecutor;
import com.quant.trade.support.AndroidUiNodeLocator;
import com.quant.trade.support.UiHierarchyParser;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于配置驱动的通用券商 APP 适配器。
 */
public class ConfiguredBrokerAppDriver implements BrokerAppDriver {

    private final String brokerCode;
    private final AndroidTradeProperties androidTradeProperties;
    private final AndroidTradeProperties.BrokerUiProfileProperties profile;
    private final AdbCommandExecutor adbCommandExecutor;
    private final UiHierarchyParser uiHierarchyParser;
    private final AndroidUiNodeLocator androidUiNodeLocator;

    public ConfiguredBrokerAppDriver(String brokerCode,
                                     AndroidTradeProperties androidTradeProperties,
                                     AndroidTradeProperties.BrokerUiProfileProperties profile,
                                     AdbCommandExecutor adbCommandExecutor,
                                     UiHierarchyParser uiHierarchyParser,
                                     AndroidUiNodeLocator androidUiNodeLocator) {
        this.brokerCode = brokerCode;
        this.androidTradeProperties = androidTradeProperties;
        this.profile = profile;
        this.adbCommandExecutor = adbCommandExecutor;
        this.uiHierarchyParser = uiHierarchyParser;
        this.androidUiNodeLocator = androidUiNodeLocator;
    }

    @Override
    public String brokerCode() {
        return brokerCode;
    }

    @Override
    public List<RealtimeHoldingEntity> captureHoldings(String accountId) {
        launchAndNavigate(profile.getHoldingsTabText(), null);
        String xml = adbCommandExecutor.dumpCurrentScreenXml();
        List<UiNode> nodes = uiHierarchyParser.parse(xml);
        String textBlock = nodes.stream()
                .map(UiNode::displayText)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));

        Pattern pattern = compileRequiredRegex(profile.getHoldingsRowRegex(), "holdingsRowRegex");
        Matcher matcher = pattern.matcher(textBlock);
        List<RealtimeHoldingEntity> result = new ArrayList<>();
        LocalDateTime snapshotTime = LocalDateTime.now();
        while (matcher.find()) {
            String stockCode = requiredGroup(matcher, "stockCode");
            RealtimeHoldingEntity entity = new RealtimeHoldingEntity();
            entity.setAccountId(accountId);
            entity.setBrokerCode(brokerCode);
            entity.setStockCode(stockCode);
            entity.setTsCode(toTsCode(stockCode));
            entity.setStockName(optionalGroup(matcher, "stockName"));
            entity.setQuantity(parseLong(optionalGroup(matcher, "quantity")));
            entity.setAvailableQuantity(parseLong(optionalGroup(matcher, "availableQuantity")));
            entity.setCostPrice(parseDecimal(optionalGroup(matcher, "costPrice")));
            entity.setLastPrice(parseDecimal(optionalGroup(matcher, "lastPrice")));
            entity.setMarketValue(parseDecimal(optionalGroup(matcher, "marketValue")));
            entity.setProfitAmount(parseDecimal(optionalGroup(matcher, "profitAmount")));
            entity.setProfitRatio(parseDecimal(optionalGroup(matcher, "profitRatio")));
            entity.setSnapshotTime(snapshotTime);
            entity.setRawPayload(matcher.group());
            result.add(entity);
        }
        if (result.isEmpty()) {
            throw new IllegalStateException("no holdings matched by configured regex for broker=" + brokerCode);
        }
        return result;
    }

    @Override
    public TradeRecordEntity submitOrder(TradeRecordEntity tradeRecord) {
        boolean buy = "BUY".equalsIgnoreCase(tradeRecord.getSide());
        launchAndNavigate(profile.getTradeTabText(), buy ? profile.getBuyTabText() : profile.getSellTabText());

        fillField(profile.getCodeFieldResourceId(), profile.getCodeFieldText(), tradeRecord.getStockCode());
        fillField(profile.getPriceFieldResourceId(), profile.getPriceFieldText(), tradeRecord.getLimitPrice().stripTrailingZeros().toPlainString());
        fillField(profile.getQuantityFieldResourceId(), profile.getQuantityFieldText(), String.valueOf(tradeRecord.getRequestedQuantity()));
        tapByText(profile.getSubmitButtonText());
        tapByText(profile.getConfirmButtonText());

        String xml = adbCommandExecutor.dumpCurrentScreenXml();
        String brokerOrderNo = extractOptionalRegex(xml, profile.getBrokerOrderNoRegex(), "brokerOrderNo");
        tradeRecord.setBrokerOrderNo(StringUtils.defaultString(brokerOrderNo));
        tradeRecord.setExecutedPrice(tradeRecord.getLimitPrice());
        tradeRecord.setExecutedQuantity(tradeRecord.getRequestedQuantity());
        tradeRecord.setStatus(TradeOrderStatusEnum.SUCCESS.name());
        tradeRecord.setStatusMessage("submitted by adb automation");
        tradeRecord.setRawPayload(xml);
        tradeRecord.setExecutedAt(LocalDateTime.now());
        return tradeRecord;
    }

    private void launchAndNavigate(String firstTabText, String secondTabText) {
        if (StringUtils.isBlank(profile.getPackageName())) {
            throw new IllegalStateException("packageName is required for broker=" + brokerCode);
        }
        adbCommandExecutor.launchApp(profile.getPackageName(), profile.getLaunchActivity());
        sleepQuietly(androidTradeProperties.getTapSettleMs());
        tapByText(firstTabText);
        if (StringUtils.isNotBlank(secondTabText)) {
            tapByText(secondTabText);
        }
    }

    private void fillField(String resourceId, String fallbackText, String value) {
        List<UiNode> nodes = uiHierarchyParser.parse(adbCommandExecutor.dumpCurrentScreenXml());
        UiNode target = androidUiNodeLocator.findByResourceId(nodes, resourceId);
        if (target == null) {
            target = androidUiNodeLocator.findByTextContains(nodes, fallbackText);
        }
        if (target == null) {
            throw new IllegalStateException("failed to locate input field, resourceId=" + resourceId + ", text=" + fallbackText);
        }
        int[] point = androidUiNodeLocator.center(target);
        adbCommandExecutor.tap(point[0], point[1]);
        adbCommandExecutor.pressDelete(12);
        adbCommandExecutor.inputText(value);
        sleepQuietly(androidTradeProperties.getTapSettleMs());
    }

    private void tapByText(String expectedText) {
        if (StringUtils.isBlank(expectedText)) {
            return;
        }
        List<UiNode> nodes = uiHierarchyParser.parse(adbCommandExecutor.dumpCurrentScreenXml());
        UiNode target = androidUiNodeLocator.findByTextContains(nodes, expectedText);
        if (target == null) {
            throw new IllegalStateException("failed to locate node by text=" + expectedText + ", broker=" + brokerCode);
        }
        int[] point = androidUiNodeLocator.center(target);
        adbCommandExecutor.tap(point[0], point[1]);
        sleepQuietly(androidTradeProperties.getTapSettleMs());
    }

    private Pattern compileRequiredRegex(String regex, String fieldName) {
        if (StringUtils.isBlank(regex)) {
            throw new IllegalStateException(fieldName + " is required for broker=" + brokerCode);
        }
        return Pattern.compile(regex, Pattern.MULTILINE);
    }

    private String extractOptionalRegex(String source, String regex, String groupName) {
        if (StringUtils.isBlank(regex)) {
            return "";
        }
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(source);
        if (!matcher.find()) {
            return "";
        }
        return optionalGroup(matcher, groupName);
    }

    private String optionalGroup(Matcher matcher, String name) {
        try {
            return StringUtils.trimToEmpty(matcher.group(name));
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String requiredGroup(Matcher matcher, String name) {
        String value = optionalGroup(matcher, name);
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException("required regex group is blank: " + name);
        }
        return value;
    }

    private String toTsCode(String stockCode) {
        StockExchangeEnum exchangeEnum = StockExchangeEnum.fromStockCode(stockCode);
        if (exchangeEnum == StockExchangeEnum.UNKNOWN) {
            return stockCode;
        }
        return stockCode + "." + exchangeEnum.getSymbolPrefix();
    }

    private Long parseLong(String value) {
        String normalized = normalizeNumber(value);
        if (StringUtils.isBlank(normalized)) {
            return 0L;
        }
        return Long.parseLong(normalized.contains(".") ? normalized.substring(0, normalized.indexOf('.')) : normalized);
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = normalizeNumber(value);
        if (StringUtils.isBlank(normalized)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(normalized);
    }

    private String normalizeNumber(String value) {
        return StringUtils.trimToEmpty(value)
                .replace(",", "")
                .replace("%", "")
                .replace("股", "")
                .replace("元", "");
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
