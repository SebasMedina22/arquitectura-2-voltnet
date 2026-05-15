package com.voltnet.orchestrator.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StopSessionRequest(
        @NotNull @PositiveOrZero BigDecimal kwhConsumed
) {
}
