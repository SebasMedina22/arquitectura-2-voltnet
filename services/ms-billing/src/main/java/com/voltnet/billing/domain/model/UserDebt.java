package com.voltnet.billing.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Snapshot de la situacion de deuda de un usuario.
 * Es lo que MS-Billing publica via UserDebtUpdatedEvent para alimentar
 * la proyeccion local de solvencia de MS-ChargeOrchestrator (R2).
 */
public final class UserDebt {

    private final UserId userId;
    private final int overdueDays;
    private final boolean hasActivePaymentMethod;
    private final Instant lastUpdatedAt;

    private UserDebt(UserId userId, int overdueDays, boolean hasActivePaymentMethod, Instant lastUpdatedAt) {
        this.userId = userId;
        this.overdueDays = overdueDays;
        this.hasActivePaymentMethod = hasActivePaymentMethod;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static UserDebt of(UserId userId,
                              int overdueDays,
                              boolean hasActivePaymentMethod,
                              Instant lastUpdatedAt) {
        Objects.requireNonNull(userId, "userId requerido");
        Objects.requireNonNull(lastUpdatedAt, "lastUpdatedAt requerido");
        if (overdueDays < 0) {
            throw new IllegalArgumentException("overdueDays no puede ser negativo: " + overdueDays);
        }
        return new UserDebt(userId, overdueDays, hasActivePaymentMethod, lastUpdatedAt);
    }

    public UserId userId() { return userId; }
    public int overdueDays() { return overdueDays; }
    public boolean hasActivePaymentMethod() { return hasActivePaymentMethod; }
    public Instant lastUpdatedAt() { return lastUpdatedAt; }
}
