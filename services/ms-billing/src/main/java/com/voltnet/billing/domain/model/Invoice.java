package com.voltnet.billing.domain.model;

import com.voltnet.billing.domain.exception.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado: factura asociada a una sesion de carga.
 *
 * Maquina de estados (sustenta el lado consumidor de R3):
 *   PENDING -> PAID  (pago recibido)
 *   PENDING -> OVERDUE  (vencimiento sin pago)
 *   OVERDUE -> PAID  (pago tardio)
 *
 * Idempotencia: una sola Invoice por sessionId, garantizado por UNIQUE en DB.
 */
public final class Invoice {

    private final UUID id;
    private final SessionId sessionId;
    private final UserId userId;
    private final Money amount;
    private final Instant createdAt;
    private final Instant dueAt;
    private InvoiceStatus status;
    private Instant paidAt;

    private Invoice(UUID id,
                    SessionId sessionId,
                    UserId userId,
                    Money amount,
                    Instant createdAt,
                    Instant dueAt,
                    InvoiceStatus status,
                    Instant paidAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.dueAt = dueAt;
        this.status = status;
        this.paidAt = paidAt;
    }

    public static Invoice createPending(SessionId sessionId,
                                        UserId userId,
                                        Money amount,
                                        Instant createdAt,
                                        Instant dueAt) {
        Objects.requireNonNull(sessionId, "sessionId requerido");
        Objects.requireNonNull(userId, "userId requerido");
        Objects.requireNonNull(amount, "amount requerido");
        Objects.requireNonNull(createdAt, "createdAt requerido");
        Objects.requireNonNull(dueAt, "dueAt requerido");
        if (!dueAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("dueAt debe ser posterior a createdAt");
        }
        return new Invoice(UUID.randomUUID(), sessionId, userId, amount,
                createdAt, dueAt, InvoiceStatus.PENDING, null);
    }

    public static Invoice rehydrate(UUID id,
                                    SessionId sessionId,
                                    UserId userId,
                                    Money amount,
                                    Instant createdAt,
                                    Instant dueAt,
                                    InvoiceStatus status,
                                    Instant paidAt) {
        return new Invoice(id, sessionId, userId, amount, createdAt, dueAt, status, paidAt);
    }

    public void markOverdue(Instant now) {
        if (status != InvoiceStatus.PENDING) {
            throw new InvalidStateTransitionException(status, InvoiceStatus.OVERDUE);
        }
        if (now.isBefore(dueAt)) {
            throw new IllegalStateException(
                    "No se puede marcar overdue antes de dueAt (now=" + now + ", dueAt=" + dueAt + ")");
        }
        this.status = InvoiceStatus.OVERDUE;
    }

    public void markPaid(Instant paidAt) {
        if (status == InvoiceStatus.PAID) {
            throw new InvalidStateTransitionException(status, InvoiceStatus.PAID);
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt;
    }

    public boolean isOverdueAt(Instant now) {
        return status == InvoiceStatus.PENDING && !now.isBefore(dueAt);
    }

    public UUID id() { return id; }
    public SessionId sessionId() { return sessionId; }
    public UserId userId() { return userId; }
    public Money amount() { return amount; }
    public Instant createdAt() { return createdAt; }
    public Instant dueAt() { return dueAt; }
    public InvoiceStatus status() { return status; }
    public Instant paidAt() { return paidAt; }
}
