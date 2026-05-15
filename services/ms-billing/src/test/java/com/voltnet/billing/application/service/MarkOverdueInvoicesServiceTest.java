package com.voltnet.billing.application.service;

import com.voltnet.billing.application.InMemoryInvoiceRepository;
import com.voltnet.billing.application.InMemoryUserDebtRepository;
import com.voltnet.billing.application.RecordingEventPublisher;
import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.InvoiceStatus;
import com.voltnet.billing.domain.model.Money;
import com.voltnet.billing.domain.model.SessionId;
import com.voltnet.billing.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkOverdueInvoicesServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-14T16:10:00Z");

    private InMemoryInvoiceRepository invoices;
    private InMemoryUserDebtRepository debts;
    private RecordingEventPublisher publisher;
    private MarkOverdueInvoicesService service;

    @BeforeEach
    void setUp() {
        invoices = new InMemoryInvoiceRepository();
        debts = new InMemoryUserDebtRepository();
        publisher = new RecordingEventPublisher();
        service = new MarkOverdueInvoicesService(
                invoices, debts, publisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Invoice savePending(String session, String user, Instant createdAt, Instant dueAt) {
        Invoice inv = Invoice.createPending(
                SessionId.of(session),
                UserId.of(user),
                Money.of(new BigDecimal("100.00"), "COP"),
                createdAt,
                dueAt
        );
        return invoices.save(inv);
    }

    @Test
    void marca_overdue_facturas_pending_vencidas() {
        savePending("S-1", "U-001", NOW.minus(Duration.ofMinutes(5)), NOW.minus(Duration.ofMinutes(1)));
        savePending("S-2", "U-002", NOW.minus(Duration.ofMinutes(10)), NOW.minus(Duration.ofMinutes(5)));

        int count = service.markAllOverdue();

        assertEquals(2, count);
        invoices.findOverdueCandidates(NOW);
        long pendingRemaining = invoices.findByUserId(UserId.of("U-001")).stream()
                .filter(i -> i.status() == InvoiceStatus.PENDING).count();
        assertEquals(0, pendingRemaining);
    }

    @Test
    void no_toca_facturas_no_vencidas() {
        savePending("S-1", "U-001", NOW, NOW.plus(Duration.ofMinutes(2)));

        int count = service.markAllOverdue();

        assertEquals(0, count);
        assertEquals(0, publisher.events().size());
    }

    @Test
    void publica_UserDebtUpdated_por_cada_factura_marcada() {
        savePending("S-1", "U-001", NOW.minus(Duration.ofMinutes(5)), NOW.minus(Duration.ofMinutes(1)));

        service.markAllOverdue();

        assertEquals(1, publisher.events().size());
        assertEquals("U-001", publisher.events().get(0).userId());
    }

    @Test
    void overdueDays_se_calcula_desde_dueAt() {
        Instant dueAt = NOW.minus(Duration.ofDays(3));
        savePending("S-1", "U-001", dueAt.minus(Duration.ofMinutes(1)), dueAt);

        service.markAllOverdue();

        assertEquals(3, publisher.events().get(0).overdueDays());
    }
}
