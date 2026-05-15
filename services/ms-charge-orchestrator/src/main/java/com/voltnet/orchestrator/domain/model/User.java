package com.voltnet.orchestrator.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Proyeccion local de la solvencia del usuario. Esta tabla en MySQL es
 * alimentada por el evento UserDebtUpdated publicado por MS-Billing.
 * Es la fuente de verdad de R2 para Orchestrator (decisiones SIN llamada
 * sincrona a Billing).
 */
public class User {

    private final UserId id;
    private int overdueDays;
    private boolean hasActivePaymentMethod;
    private Instant lastUpdatedAt;

    public User(UserId id, int overdueDays, boolean hasActivePaymentMethod, Instant lastUpdatedAt) {
        this.id = Objects.requireNonNull(id);
        if (overdueDays < 0) {
            throw new IllegalArgumentException("overdueDays no puede ser negativo");
        }
        this.overdueDays = overdueDays;
        this.hasActivePaymentMethod = hasActivePaymentMethod;
        this.lastUpdatedAt = Objects.requireNonNull(lastUpdatedAt);
    }

    public void updateDebt(int overdueDays, boolean hasActivePaymentMethod, Instant at) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("overdueDays no puede ser negativo");
        }
        this.overdueDays = overdueDays;
        this.hasActivePaymentMethod = hasActivePaymentMethod;
        this.lastUpdatedAt = Objects.requireNonNull(at);
    }

    public UserId id() { return id; }
    public int overdueDays() { return overdueDays; }
    public boolean hasActivePaymentMethod() { return hasActivePaymentMethod; }
    public Instant lastUpdatedAt() { return lastUpdatedAt; }
}
