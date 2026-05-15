package com.voltnet.gridload.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KwTest {

    @Test
    void acepta_cero() {
        assertEquals(0.0, Kw.of(0.0).value());
    }

    @Test
    void acepta_valor_positivo() {
        assertEquals(45.3, Kw.of(45.3).value());
    }

    @Test
    void rechaza_negativo() {
        assertThrows(IllegalArgumentException.class, () -> Kw.of(-0.1));
    }

    @Test
    void rechaza_NaN() {
        assertThrows(IllegalArgumentException.class, () -> Kw.of(Double.NaN));
    }

    @Test
    void rechaza_infinito() {
        assertThrows(IllegalArgumentException.class, () -> Kw.of(Double.POSITIVE_INFINITY));
    }
}
