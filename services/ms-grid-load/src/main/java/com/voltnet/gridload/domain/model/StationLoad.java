package com.voltnet.gridload.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Agregado: snapshot de carga de una estacion.
 * RG-3: isOverloaded() es true cuando la carga supera el umbral de 100 kW.
 * Este umbral lo evalua el dominio, no el controller.
 */
public final class StationLoad {

    public static final double OVERLOAD_THRESHOLD_KW = 100.0;

    private final StationId stationId;
    private final Kw currentLoad;
    private final Instant timestamp;

    private StationLoad(StationId stationId, Kw currentLoad, Instant timestamp) {
        this.stationId = stationId;
        this.currentLoad = currentLoad;
        this.timestamp = timestamp;
    }

    public static StationLoad of(StationId stationId, Kw currentLoad, Instant timestamp) {
        Objects.requireNonNull(stationId, "stationId requerido");
        Objects.requireNonNull(currentLoad, "currentLoad requerido");
        Objects.requireNonNull(timestamp, "timestamp requerido");
        return new StationLoad(stationId, currentLoad, timestamp);
    }

    public boolean isOverloaded() {
        return currentLoad.value() > OVERLOAD_THRESHOLD_KW;
    }

    public StationId stationId() {
        return stationId;
    }

    public Kw currentLoad() {
        return currentLoad;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
