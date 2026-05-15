package com.voltnet.orchestrator.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record StartSessionRequest(
        @NotBlank String userId,
        @NotBlank String stationId
) {
}
