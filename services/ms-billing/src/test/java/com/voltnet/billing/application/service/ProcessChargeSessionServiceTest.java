package com.voltnet.billing.application.service;

import com.voltnet.billing.application.InMemoryInvoiceRepository;
import com.voltnet.billing.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.InvoiceStatus;
import com.voltnet.billing.domain.model.SessionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProcessChargeSessionServiceTest {

    private static final Instant FIXED = Instant.parse("2026-05-14T16:00:00Z");

    private InMemoryInvoiceRepository invoices;
    private ProcessChargeSessionService service;

    @BeforeEach
    void setUp() {
        invoices = new InMemoryInvoiceRepository();
        service = new ProcessChargeSessionService(
                invoices,
                Clock.fixed(FIXED, ZoneOffset.UTC),
                new BigDecimal("500.00"),
                "COP",
                Duration.ofMinutes(2)
        );
    }

    @Test
    void crea_factura_PENDING_a_partir_del_evento() {
        ChargeSessionCompletedEvent event = new ChargeSessionCompletedEvent(
                "S-001", "USR-042", "STN-001", 10.0, FIXED);

        Invoice result = service.process(event);

        assertEquals(InvoiceStatus.PENDING, result.status());
        assertEquals(new BigDecimal("5000.00"), result.amount().amount());
        assertEquals("COP", result.amount().currency());
        assertEquals(FIXED.plus(Duration.ofMinutes(2)), result.dueAt());
    }

    @Test
    void es_idempotente_para_mismo_sessionId() {
        ChargeSessionCompletedEvent event = new ChargeSessionCompletedEvent(
                "S-001", "USR-042", "STN-001", 10.0, FIXED);

        Invoice first  = service.process(event);
        Invoice second = service.process(event);

        // Mismo agregado, no duplicado.
        assertSame(invoices.findBySessionId(SessionId.of("S-001")).orElseThrow().id(),
                   first.id());
        assertEquals(first.id(), second.id());

        // Solo una factura para ese sessionId.
        List<Invoice> all = invoices.findByUserId(first.userId());
        assertEquals(1, all.size());
    }

    @Test
    void rechaza_userId_invalido_via_value_object() {
        ChargeSessionCompletedEvent bad = new ChargeSessionCompletedEvent(
                "S-002", "no-user-id-valido", "STN-001", 5.0, FIXED);
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.process(bad));
    }
}
