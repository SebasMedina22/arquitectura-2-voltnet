package com.voltnet.orchestrator.domain.model;

import java.math.BigDecimal;

/**
 * Snapshot inmutable de la carga de una estacion segun MS-GridLoad.
 * Lo retorna el puerto GridLoadPort y lo consume GridCapacityPolicy (R1).
 */
public record StationLoadSnapshot(StationId stationId, BigDecimal totalLoadKw, boolean overloaded) {
}
