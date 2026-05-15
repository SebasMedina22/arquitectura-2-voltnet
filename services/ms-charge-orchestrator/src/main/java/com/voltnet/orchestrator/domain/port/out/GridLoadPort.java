package com.voltnet.orchestrator.domain.port.out;

import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.StationLoadSnapshot;

/**
 * Puerto de salida hacia MS-GridLoad. Implementado por un Adapter Feign
 * en infrastructure/. El dominio NO conoce HTTP ni Feign.
 */
public interface GridLoadPort {
    StationLoadSnapshot fetchLoad(StationId stationId);
}
