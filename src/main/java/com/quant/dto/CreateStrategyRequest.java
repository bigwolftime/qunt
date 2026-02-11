package com.quant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStrategyRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 1024) String description
) {
}
