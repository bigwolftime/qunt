package com.quant.dto;

import java.time.Instant;

public record StrategyResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
) {
}
