package com.voltnet.orchestrator.domain.event;

import java.time.Instant;

/**
 * Evento entrante publicado por MS-Billing. El Orchestrator lo consume
 * para mantener actualizada la proyeccion local de solvencia (tabla users),
 * sin necesidad de llamar sincronamente a Billing en cada start de carga.
 */
public record UserDebtUpdatedEvent(
        String userId,
        int overdueDays,
        boolean hasActivePaymentMethod,
        Instant updatedAt
) {
}
