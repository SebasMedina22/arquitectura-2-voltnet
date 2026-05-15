package com.voltnet.gridload.infrastructure.rest.dto;

import com.voltnet.gridload.domain.model.StationLoad;

import java.time.Instant;

public record GridLoadResponse(
        String stationId,
        double currentLoadKw,
        boolean overloaded,
        Instant timestamp
) {

    public static GridLoadResponse fromDomain(StationLoad load) {
        return new GridLoadResponse(
                load.stationId().value(),
                load.currentLoad().value(),
                load.isOverloaded(),
                load.timestamp()
        );
    }
}
