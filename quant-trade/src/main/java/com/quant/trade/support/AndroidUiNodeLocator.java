package com.quant.trade.support;

import com.quant.trade.dto.UiNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UI 节点查找器。
 */
@Component
public class AndroidUiNodeLocator {

    public UiNode findByTextContains(List<UiNode> nodes, String expectedText) {
        if (StringUtils.isBlank(expectedText)) {
            return null;
        }
        String normalized = expectedText.trim();
        return nodes.stream()
                .filter(node -> StringUtils.contains(node.displayText(), normalized))
                .findFirst()
                .orElse(null);
    }

    public UiNode findByResourceId(List<UiNode> nodes, String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            return null;
        }
        return nodes.stream()
                .filter(node -> resourceId.equals(node.resourceId()))
                .findFirst()
                .orElse(null);
    }

    public int[] center(UiNode node) {
        if (node == null || StringUtils.isBlank(node.bounds())) {
            throw new IllegalArgumentException("ui node bounds is blank");
        }
        String value = node.bounds().replace("][", ",").replace("[", "").replace("]", "");
        String[] parts = value.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("invalid bounds: " + node.bounds());
        }
        int left = Integer.parseInt(parts[0]);
        int top = Integer.parseInt(parts[1]);
        int right = Integer.parseInt(parts[2]);
        int bottom = Integer.parseInt(parts[3]);
        return new int[]{(left + right) / 2, (top + bottom) / 2};
    }
}
