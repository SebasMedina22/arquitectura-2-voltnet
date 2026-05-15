package com.voltnet.billing.application.service;

import com.voltnet.billing.application.port.in.ProcessChargeSessionUseCase;
import com.voltnet.billing.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.Money;
import com.voltnet.billing.domain.model.SessionId;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.InvoiceRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Caso de uso: procesar el evento ChargeSessionCompleted y crear factura.
 *
 * Sustento de R3 (lado consumidor):
 *  - Idempotencia: si existe factura para sessionId, devuelve la existente sin
 *    crear duplicado. El cerrojo definitivo lo da UNIQUE(session_id) en DB.
 *  - El consumer AMQP captura DuplicateInvoiceException y hace ACK silencioso,
 *    aislando reintentos del broker.
 */
public class ProcessChargeSessionService implements ProcessChargeSessionUseCase {

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;
    private final BigDecimal ratePerKwh;
    private final String currency;
    private final Duration dueAfter;

    public ProcessChargeSessionService(InvoiceRepository invoiceRepository,
                                       Clock clock,
                                       BigDecimal ratePerKwh,
                                       String currency,
                                       Duration dueAfter) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
        this.ratePerKwh = ratePerKwh;
        this.currency = currency;
        this.dueAfter = dueAfter;
    }

    @Override
    public Invoice process(ChargeSessionCompletedEvent event) {
        SessionId sessionId = SessionId.of(event.sessionId());

        return invoiceRepository.findBySessionId(sessionId)
                .orElseGet(() -> createNewInvoice(event, sessionId));
    }

    private Invoice createNewInvoice(ChargeSessionCompletedEvent event, SessionId sessionId) {
        UserId userId = UserId.of(event.userId());
        Money amount = Money.ofKwhTimesRate(event.kwhConsumed(), ratePerKwh, currency);
        Instant createdAt = Instant.now(clock);
        Instant dueAt = createdAt.plus(dueAfter);
        Invoice invoice = Invoice.createPending(sessionId, userId, amount, createdAt, dueAt);
        return invoiceRepository.save(invoice);
    }
}
