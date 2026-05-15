package com.voltnet.gridload.application.service;

import com.voltnet.gridload.application.port.in.SetStationLoadUseCase;
import com.voltnet.gridload.domain.model.Kw;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;
import com.voltnet.gridload.domain.port.out.StationLoadPort;

public class SetStationLoadService implements SetStationLoadUseCase {

    private final StationLoadPort stationLoadPort;

    public SetStationLoadService(StationLoadPort stationLoadPort) {
        this.stationLoadPort = stationLoadPort;
    }

    @Override
    public StationLoad setLoad(String rawStationId, double currentLoadKw) {
        StationId stationId = StationId.of(rawStationId);
        Kw kw = Kw.of(currentLoadKw);
        return stationLoadPort.save(stationId, kw);
    }
}
