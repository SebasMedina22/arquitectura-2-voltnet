package com.voltnet.billing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIdTest {

    @Test
    void acepta_formato_valido() {
        assertEquals("USR-042", UserId.of("USR-042").value());
    }

    @Test
    void normaliza_a_mayusculas() {
        assertEquals("USR-ABC", UserId.of("usr-abc").value());
    }

    @Test
    void rechaza_null() {
        assertThrows(IllegalArgumentException.class, () -> UserId.of(null));
    }

    @Test
    void rechaza_vacio() {
        assertThrows(IllegalArgumentException.class, () -> UserId.of(""));
    }

    @Test
    void rechaza_formato_invalido() {
        assertThrows(IllegalArgumentException.class, () -> UserId.of("USER-1"));
        assertThrows(IllegalArgumentException.class, () -> UserId.of("USR-"));
        assertThrows(IllegalArgumentException.class, () -> UserId.of("U-001"));
        assertThrows(IllegalArgumentException.class, () -> UserId.of("42"));
    }
}
