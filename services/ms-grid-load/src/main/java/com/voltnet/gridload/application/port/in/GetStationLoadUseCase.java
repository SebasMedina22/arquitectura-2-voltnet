package com.voltnet.gridload.application.port.in;

import com.voltnet.gridload.domain.model.StationLoad;

public interface GetStationLoadUseCase {

    StationLoad getLoad(String stationId);
}
