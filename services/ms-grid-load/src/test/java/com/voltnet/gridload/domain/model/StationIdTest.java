package com.voltnet.gridload.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StationIdTest {

    @Test
    void acepta_formato_valido() {
        assertEquals("STN-001", StationId.of("STN-001").value());
    }

    @Test
    void rechaza_null() {
        assertThrows(IllegalArgumentException.class, () -> StationId.of(null));
    }

    @Test
    void rechaza_vacio() {
        assertThrows(IllegalArgumentException.class, () -> StationId.of(""));
    }

    @Test
    void rechaza_blank() {
        assertThrows(IllegalArgumentException.class, () -> StationId.of("   "));
    }

    @Test
    void rechaza_formato_invalido() {
        assertThrows(IllegalArgumentException.class, () -> StationId.of("ABC-001"));
        assertThrows(IllegalArgumentException.class, () -> StationId.of("STN-1"));
        assertThrows(IllegalArgumentException.class, () -> StationId.of("STN-0001"));
    }
}
