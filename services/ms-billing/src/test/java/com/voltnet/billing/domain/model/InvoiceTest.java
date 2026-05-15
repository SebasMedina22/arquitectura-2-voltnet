package com.voltnet.billing.domain.model;

import com.voltnet.billing.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-05-14T16:00:00Z");
    private static final Instant DUE = NOW.plus(Duration.ofMinutes(2));

    private Invoice newPending() {
        return Invoice.createPending(
                SessionId.of("S-001"),
                UserId.of("U-042"),
                Money.of(new BigDecimal("5000.00"), "COP"),
                NOW,
                DUE
        );
    }

    @Test
    void se_crea_en_estado_PENDING() {
        Invoice inv = newPending();
        assertEquals(InvoiceStatus.PENDING, inv.status());
    }

    @Test
    void PENDING_a_OVERDUE_es_valido_despues_de_dueAt() {
        Invoice inv = newPending();
        inv.markOverdue(DUE.plusSeconds(1));
        assertEquals(InvoiceStatus.OVERDUE, inv.status());
    }

    @Test
    void no_se_puede_marcar_OVERDUE_antes_de_dueAt() {
        Invoice inv = newPending();
        assertThrows(IllegalStateException.class, () -> inv.markOverdue(NOW.plusSeconds(30)));
    }

    @Test
    void PENDING_a_PAID_es_valido() {
        Invoice inv = newPending();
        Instant paidAt = NOW.plusSeconds(60);
        inv.markPaid(paidAt);
        assertEquals(InvoiceStatus.PAID, inv.status());
        assertEquals(paidAt, inv.paidAt());
    }

    @Test
    void OVERDUE_a_PAID_es_valido() {
        Invoice inv = newPending();
        inv.markOverdue(DUE.plusSeconds(1));
        inv.markPaid(DUE.plusSeconds(60));
        assertEquals(InvoiceStatus.PAID, inv.status());
    }

    @Test
    void PAID_no_puede_volver_a_PAID() {
        Invoice inv = newPending();
        inv.markPaid(NOW.plusSeconds(60));
        assertThrows(InvalidStateTransitionException.class,
                () -> inv.markPaid(NOW.plusSeconds(120)));
    }

    @Test
    void PAID_no_puede_pasar_a_OVERDUE() {
        Invoice inv = newPending();
        inv.markPaid(NOW.plusSeconds(60));
        assertThrows(InvalidStateTransitionException.class,
                () -> inv.markOverdue(DUE.plusSeconds(1)));
    }

    @Test
    void dueAt_debe_ser_posterior_a_createdAt() {
        assertThrows(IllegalArgumentException.class,
                () -> Invoice.createPending(SessionId.of("S-1"), UserId.of("U-042"),
                        Money.of(BigDecimal.TEN, "COP"), NOW, NOW));
    }

    @Test
    void isOverdueAt_devuelve_true_para_PENDING_vencida() {
        Invoice inv = newPending();
        assertTrue(inv.isOverdueAt(DUE.plusSeconds(1)));
    }

    @Test
    void isOverdueAt_devuelve_false_para_PENDING_no_vencida() {
        Invoice inv = newPending();
        assertFalse(inv.isOverdueAt(NOW.plusSeconds(30)));
    }

    @Test
    void se_le_asigna_un_id() {
        Invoice inv = newPending();
        assertNotNull(inv.id());
    }
}
