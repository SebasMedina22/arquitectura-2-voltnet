package com.voltnet.gridload.application.service;

import com.voltnet.gridload.application.InMemoryStationLoadAdapter;
import com.voltnet.gridload.domain.model.StationLoad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetStationLoadServiceTest {

    private InMemoryStationLoadAdapter adapter;
    private SetStationLoadService service;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryStationLoadAdapter();
        service = new SetStationLoadService(adapter);
    }

    @Test
    void persiste_carga_valida_y_marca_no_sobrecargada() {
        StationLoad result = service.setLoad("STN-001", 50.0);
        assertEquals(50.0, result.currentLoad().value());
        assertFalse(result.isOverloaded());
    }

    @Test
    void marca_sobrecargada_cuando_supera_100() {
        StationLoad result = service.setLoad("STN-001", 110.0);
        assertTrue(result.isOverloaded());
    }

    @Test
    void rechaza_kw_negativo() {
        assertThrows(IllegalArgumentException.class, () -> service.setLoad("STN-001", -5.0));
    }

    @Test
    void rechaza_stationId_invalido() {
        assertThrows(IllegalArgumentException.class, () -> service.setLoad("XYZ", 50.0));
    }
}
