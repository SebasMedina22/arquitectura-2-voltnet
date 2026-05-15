package com.voltnet.gridload.application.port.in;

import com.voltnet.gridload.domain.model.StationLoad;

public interface SetStationLoadUseCase {

    StationLoad setLoad(String stationId, double currentLoadKw);
}
