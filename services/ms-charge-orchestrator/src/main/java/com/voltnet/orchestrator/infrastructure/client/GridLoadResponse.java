package com.voltnet.orchestrator.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO de respuesta del endpoint GET /grid/load de MS-GridLoad.
 * Campos alineados con el contrato real de la Fase 2.
 */
public record GridLoadResponse(
        @JsonProperty("stationId") String stationId,
        @JsonProperty("totalLoadKw") BigDecimal totalLoadKw,
        @JsonProperty("overloaded") boolean overloaded
) {
}
