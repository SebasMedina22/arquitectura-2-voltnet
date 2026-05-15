package com.voltnet.billing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void acepta_monto_positivo_y_currency_valido() {
        Money m = Money.of(new BigDecimal("100.50"), "COP");
        assertEquals(new BigDecimal("100.50"), m.amount());
        assertEquals("COP", m.currency());
    }

    @Test
    void acepta_cero() {
        Money m = Money.of(BigDecimal.ZERO, "COP");
        assertEquals(new BigDecimal("0.00"), m.amount());
    }

    @Test
    void rechaza_amount_negativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("-1.00"), "COP"));
    }

    @Test
    void rechaza_currency_vacio() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(BigDecimal.TEN, ""));
    }

    @Test
    void rechaza_currency_longitud_invalida() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(BigDecimal.TEN, "PESO"));
    }

    @Test
    void calcula_kwh_por_tarifa() {
        Money m = Money.ofKwhTimesRate(10.0, new BigDecimal("500.00"), "COP");
        assertEquals(new BigDecimal("5000.00"), m.amount());
    }

    @Test
    void rechaza_kwh_negativo_en_calculo() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.ofKwhTimesRate(-1.0, new BigDecimal("500"), "COP"));
    }
}
