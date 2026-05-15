package com.voltnet.gridload.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationLoadTest {

    @Test
    void no_esta_sobrecargada_bajo_el_umbral() {
        StationLoad load = StationLoad.of(
                StationId.of("STN-001"), Kw.of(45.3), Instant.now());
        assertFalse(load.isOverloaded());
    }

    @Test
    void no_esta_sobrecargada_en_el_umbral_exacto() {
        StationLoad load = StationLoad.of(
                StationId.of("STN-001"), Kw.of(100.0), Instant.now());
        assertFalse(load.isOverloaded());
    }

    @Test
    void esta_sobrecargada_sobre_el_umbral() {
        StationLoad load = StationLoad.of(
                StationId.of("STN-001"), Kw.of(100.1), Instant.now());
        assertTrue(load.isOverloaded());
    }
}
