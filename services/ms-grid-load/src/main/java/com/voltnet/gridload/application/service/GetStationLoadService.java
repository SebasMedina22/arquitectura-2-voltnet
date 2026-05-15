package com.voltnet.gridload.application.service;

import com.voltnet.gridload.application.port.in.GetStationLoadUseCase;
import com.voltnet.gridload.domain.exception.StationNotFoundException;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;
import com.voltnet.gridload.domain.port.out.StationLoadPort;

public class GetStationLoadService implements GetStationLoadUseCase {

    private final StationLoadPort stationLoadPort;

    public GetStationLoadService(StationLoadPort stationLoadPort) {
        this.stationLoadPort = stationLoadPort;
    }

    @Override
    public StationLoad getLoad(String rawStationId) {
        StationId stationId = StationId.of(rawStationId);
        return stationLoadPort.findByStationId(stationId)
                .orElseThrow(() -> new StationNotFoundException(rawStationId));
    }
}
