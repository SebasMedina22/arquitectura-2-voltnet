package com.voltnet.gridload.domain.exception;

public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(String stationId) {
        super("Estacion no encontrada: " + stationId);
    }
}
