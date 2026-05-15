package com.voltnet.gridload.application.service;

import com.voltnet.gridload.application.InMemoryStationLoadAdapter;
import com.voltnet.gridload.domain.exception.StationNotFoundException;
import com.voltnet.gridload.domain.model.Kw;
import com.voltnet.gridload.domain.model.StationId;
import com.voltnet.gridload.domain.model.StationLoad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetStationLoadServiceTest {

    private InMemoryStationLoadAdapter adapter;
    private GetStationLoadService service;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryStationLoadAdapter();
        service = new GetStationLoadService(adapter);
    }

    @Test
    void devuelve_carga_existente() {
        adapter.save(StationId.of("STN-001"), Kw.of(45.3));
        StationLoad load = service.getLoad("STN-001");
        assertEquals(45.3, load.currentLoad().value());
    }

    @Test
    void tira_NotFound_si_no_existe() {
        assertThrows(StationNotFoundException.class, () -> service.getLoad("STN-999"));
    }

    @Test
    void rechaza_stationId_invalido_antes_de_consultar() {
        assertThrows(IllegalArgumentException.class, () -> service.getLoad("ABC"));
    }
}
