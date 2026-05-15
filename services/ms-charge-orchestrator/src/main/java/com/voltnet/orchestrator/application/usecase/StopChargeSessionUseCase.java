package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.orchestrator.domain.exception.SessionNotFoundException;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.Kwh;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;
import com.voltnet.orchestrator.domain.port.out.DomainEventPublisher;

import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso: cerrar sesion de carga.
 *
 * Soporta R3 ("Al finalizar, el calculo de kW consumidos se envia a un sistema
 * de facturacion asincrono. La sesion se cierra exitosamente aunque el sistema
 * de facturacion este fuera de linea").
 *
 * Estrategia: en una sola transaccion (controlada por el adaptador JPA),
 *  1) marca la sesion COMPLETED,
 *  2) escribe el evento en outbox (puerto DomainEventPublisher).
 * Si RabbitMQ esta caido, el worker reintenta luego: el usuario NUNCA queda
 * bloqueado.
 */
public class StopChargeSessionUseCase {

    private final ChargeSessionRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public StopChargeSessionUseCase(ChargeSessionRepository repository,
                                    DomainEventPublisher eventPublisher,
                                    Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public ChargeSession execute(SessionId sessionId, Kwh kwhConsumed) {
        ChargeSession session = repository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesion " + sessionId + " no encontrada"));

        Instant completedAt = Instant.now(clock);
        session.complete(kwhConsumed, completedAt);
        repository.save(session);

        eventPublisher.publish(new ChargeSessionCompletedEvent(
                session.id().value(),
                session.userId().value(),
                session.stationId().value(),
                kwhConsumed.doubleValue(),
                completedAt
        ));
        return session;
    }
}
