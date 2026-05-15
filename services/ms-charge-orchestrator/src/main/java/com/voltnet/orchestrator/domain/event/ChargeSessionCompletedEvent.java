package com.voltnet.orchestrator.domain.event;

import java.time.Instant;

/**
 * Evento saliente del Orchestrator hacia el broker.
 * Contrato JSON acordado con MS-Billing (ver RabbitMqConfig de Billing,
 * routing key "charge.session.completed" en exchange "voltnet.charge.events").
 *
 * Soporta R3: se persiste primero en la tabla outbox dentro de la misma
 * transaccion que cierra la sesion. Un worker scheduleado lo publica luego.
 */
public record ChargeSessionCompletedEvent(
        String sessionId,
        String userId,
        String stationId,
        double kwhConsumed,
        Instant completedAt
) {
}
