package com.voltnet.billing.domain.event;

import java.time.Instant;

/**
 * Evento entrante publicado por MS-ChargeOrchestrator al cerrar una sesion.
 * Materializa el lado "publicador" de R3. Billing lo consume via AMQP.
 */
public record ChargeSessionCompletedEvent(
        String sessionId,
        String userId,
        String stationId,
        double kwhConsumed,
        Instant completedAt
) {
}
