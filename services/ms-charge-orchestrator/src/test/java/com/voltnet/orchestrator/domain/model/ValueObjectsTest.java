package com.voltnet.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectsTest {

    @Test
    void userId_aceptaFormatoValido() {
        assertEquals("USR-001", new UserId("USR-001").value());
    }

    @Test
    void userId_rechazaFormatoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new UserId("user-001"));
        assertThrows(IllegalArgumentException.class, () -> new UserId("USR-"));
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }

    @Test
    void stationId_aceptaFormatoValido() {
        assertEquals("STN-ABC", new StationId("STN-ABC").value());
    }

    @Test
    void stationId_rechazaFormatoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new StationId("stn-001"));
    }

    @Test
    void kwh_noPermiteNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new Kwh(new BigDecimal("-1")));
    }

    @Test
    void kwh_redondeaA3Decimales() {
        Kwh k = Kwh.of(12.34567);
        assertEquals(new BigDecimal("12.346"), k.value());
    }

    @Test
    void sessionId_noVacio() {
        assertThrows(IllegalArgumentException.class, () -> new SessionId(""));
        assertNotNull(SessionId.generate().value());
    }
}
