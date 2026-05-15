package com.voltnet.gridload.domain.port.out;

import com.voltnet.gridload.domain.model.Kw;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;

import java.util.Optional;

/**
 * Puerto de salida del dominio hacia el adaptador de persistencia.
 * En produccion lo implementa RedisStationLoadAdapter (patron Adapter).
 * En tests lo implementa un fake en memoria.
 */
public interface StationLoadPort {

    Optional<StationLoad> findByStationId(StationId stationId);

    StationLoad save(StationId stationId, Kw currentLoad);
}
