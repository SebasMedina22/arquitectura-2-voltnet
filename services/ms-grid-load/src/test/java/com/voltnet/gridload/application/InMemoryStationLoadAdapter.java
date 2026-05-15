package com.voltnet.gridload.application;

import com.voltnet.gridload.domain.model.Kw;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;
import com.voltnet.gridload.domain.port.out.StationLoadPort;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementacion en memoria del puerto, usada solo en tests.
 * Demuestra el Principio de Sustitucion de Liskov: el caso de uso no nota la diferencia.
 */
public class InMemoryStationLoadAdapter implements StationLoadPort {

    private final Map<String, Double> data = new HashMap<>();

    @Override
    public Optional<StationLoad> findByStationId(StationId stationId) {
        Double value = data.get(stationId.value());
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(StationLoad.of(stationId, Kw.of(value), Instant.now()));
    }

    @Override
    public StationLoad save(StationId stationId, Kw currentLoad) {
        data.put(stationId.value(), currentLoad.value());
        return StationLoad.of(stationId, currentLoad, Instant.now());
    }
}
