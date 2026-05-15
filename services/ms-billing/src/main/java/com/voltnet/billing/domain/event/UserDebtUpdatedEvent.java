package com.voltnet.billing.domain.event;

import com.voltnet.billing.domain.model.UserDebt;

import java.time.Instant;

/**
 * Evento saliente. Billing lo publica cada vez que cambia la situacion
 * de deuda de un usuario. Orchestrator lo consume para actualizar su
 * proyeccion local de solvencia (consumida por R2).
 */
public record UserDebtUpdatedEvent(
        String userId,
        int overdueDays,
        boolean hasActivePaymentMethod,
        Instant updatedAt
) {
    public static UserDebtUpdatedEvent fromDomain(UserDebt debt) {
        return new UserDebtUpdatedEvent(
                debt.userId().value(),
                debt.overdueDays(),
                debt.hasActivePaymentMethod(),
                debt.lastUpdatedAt()
        );
    }
}
