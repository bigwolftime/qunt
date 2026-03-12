package com.quant.trade.dto;

/**
 * Android 无障碍节点。
 */
public record UiNode(
        String text,
        String contentDesc,
        String resourceId,
        String className,
        String bounds,
        boolean clickable
) {

    public String displayText() {
        if (text != null && !text.isBlank()) {
            return text;
        }
        return contentDesc == null ? "" : contentDesc;
    }
}
