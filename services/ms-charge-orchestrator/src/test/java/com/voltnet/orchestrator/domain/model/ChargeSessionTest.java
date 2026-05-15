package com.voltnet.orchestrator.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChargeSessionTest {

    @Test
    void inicia_enEstadoStarted() {
        ChargeSession s = new ChargeSession(SessionId.generate(),
                new UserId("USR-001"), new StationId("STN-001"), Instant.now());
        assertEquals(ChargeSessionStatus.STARTED, s.status());
        assertEquals(Kwh.zero(), s.kwhConsumed());
        assertFalse(s.isCompleted());
    }

    @Test
    void complete_fijaKwhYEstado() {
        ChargeSession s = new ChargeSession(SessionId.generate(),
                new UserId("USR-001"), new StationId("STN-001"), Instant.now());
        s.complete(Kwh.of(12.5), Instant.now());
        assertTrue(s.isCompleted());
        assertEquals(Kwh.of(12.5), s.kwhConsumed());
        assertNotNull(s.completedAt());
    }

    @Test
    void completar_dosVeces_falla() {
        ChargeSession s = new ChargeSession(SessionId.generate(),
                new UserId("USR-001"), new StationId("STN-001"), Instant.now());
        s.complete(Kwh.of(1), Instant.now());
        assertThrows(IllegalStateException.class, () -> s.complete(Kwh.of(2), Instant.now()));
    }
}
