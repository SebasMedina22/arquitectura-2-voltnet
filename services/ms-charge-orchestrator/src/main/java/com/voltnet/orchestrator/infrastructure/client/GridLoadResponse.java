package com.voltnet.orchestrator.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO de respuesta del endpoint GET /grid/load de MS-GridLoad.
 * Campos alineados con el contrato real de la Fase 2.
 * ignoreUnknown = true porque Feign no hereda la configuracion Jackson de
 * Spring por defecto y MS-GridLoad anade un campo "timestamp" que aqui no
 * usamos. Mejor descartar al decodificar que romper la decision de R1.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GridLoadResponse(
        @JsonProperty("stationId") String stationId,
        @JsonProperty("currentLoadKw") BigDecimal currentLoadKw,
        @JsonProperty("overloaded") boolean overloaded
) {
}
