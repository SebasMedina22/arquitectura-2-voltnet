package com.voltnet.gridload.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetGridLoadRequest(
        @NotBlank String stationId,
        @NotNull Double currentLoadKw
) {
}
