package com.voltnet.orchestrator.infrastructure.client;

import com.voltnet.orchestrator.domain.exception.GridLoadUnavailableException;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.StationLoadSnapshot;
import com.voltnet.orchestrator.domain.port.out.GridLoadPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Patron Adapter (GoF): traduce el puerto del dominio (GridLoadPort) a una
 * llamada Feign concreta. Envuelto en Resilience4j Circuit Breaker:
 * si MS-GridLoad falla repetidamente, el breaker abre y entra al fallback,
 * que aplica la politica "en duda, rechazar" — bloquear el inicio antes
 * que arriesgar romper la red electrica.
 */
@Component
public class GridLoadFeignAdapter implements GridLoadPort {

    private static final Logger log = LoggerFactory.getLogger(GridLoadFeignAdapter.class);

    private final GridLoadFeignClient client;

    public GridLoadFeignAdapter(GridLoadFeignClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "gridload", fallbackMethod = "rejectOnDoubt")
    public StationLoadSnapshot fetchLoad(StationId stationId) {
        GridLoadResponse resp = client.fetchLoad(stationId.value());
        return new StationLoadSnapshot(stationId, resp.currentLoadKw(), resp.overloaded());
    }

    @SuppressWarnings("unused")
    private StationLoadSnapshot rejectOnDoubt(StationId stationId, Throwable t) {
        log.warn("Fallback de GridLoad para {} ({}): aplicando 'en duda rechazar'",
                stationId, t.toString());
        throw new GridLoadUnavailableException(
                "MS-GridLoad no disponible para estacion " + stationId
                        + ". Politica: en duda, rechazar.", t);
    }
}
