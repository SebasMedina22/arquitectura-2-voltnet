package com.voltnet.billing.infrastructure.rest.dto;

import com.voltnet.billing.domain.model.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String sessionId,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant dueAt,
        Instant paidAt
) {

    public static InvoiceResponse fromDomain(Invoice invoice) {
        return new InvoiceResponse(
                invoice.id(),
                invoice.sessionId().value(),
                invoice.userId().value(),
                invoice.amount().amount(),
                invoice.amount().currency(),
                invoice.status().name(),
                invoice.createdAt(),
                invoice.dueAt(),
                invoice.paidAt()
        );
    }
}
