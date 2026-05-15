package com.voltnet.gridload.application.config;

import com.voltnet.gridload.application.port.in.GetStationLoadUseCase;
import com.voltnet.gridload.application.port.in.SetStationLoadUseCase;
import com.voltnet.gridload.application.service.GetStationLoadService;
import com.voltnet.gridload.application.service.SetStationLoadService;
import com.voltnet.gridload.domain.port.out.StationLoadPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de casos de uso. Vive en la capa application para mantener los
 * services libres de anotaciones Spring (dominio + aplicacion son Java puro).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public GetStationLoadUseCase getStationLoadUseCase(StationLoadPort stationLoadPort) {
        return new GetStationLoadService(stationLoadPort);
    }

    @Bean
    public SetStationLoadUseCase setStationLoadUseCase(StationLoadPort stationLoadPort) {
        return new SetStationLoadService(stationLoadPort);
    }
}
