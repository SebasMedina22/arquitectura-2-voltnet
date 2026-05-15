package com.voltnet.gridload.infrastructure.config;

import com.voltnet.gridload.application.port.in.SetStationLoadUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Carga datos de demostracion al arrancar: tres estaciones con cargas variadas
 * (una sobrecargada > 100 kW) para tener escenarios de prueba listos.
 *
 * Activado solo si gridload.seed.enabled = true.
 */
@Configuration
public class SeedDataRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    @Bean
    @ConditionalOnProperty(name = "gridload.seed.enabled", havingValue = "true")
    public ApplicationRunner seedStations(SetStationLoadUseCase setStationLoad) {
        return args -> {
            log.info("Seed: cargando estaciones de demostracion...");
            try {
                setStationLoad.setLoad("STN-001", 45.3);
                setStationLoad.setLoad("STN-002", 92.8);
                setStationLoad.setLoad("STN-003", 105.0);
                log.info("Seed: STN-001=45.3 kW | STN-002=92.8 kW | STN-003=105.0 kW (sobrecargada)");
            } catch (Exception e) {
                log.warn("Seed: no se pudo cargar datos iniciales ({}). " +
                        "Si Redis no esta arriba aun, ignora este mensaje.", e.getMessage());
            }
        };
    }
}
