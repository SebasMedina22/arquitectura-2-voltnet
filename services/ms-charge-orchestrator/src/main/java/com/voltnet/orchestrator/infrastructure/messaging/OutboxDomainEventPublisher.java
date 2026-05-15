package com.voltnet.orchestrator.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltnet.orchestrator.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.orchestrator.domain.port.out.DomainEventPublisher;
import com.voltnet.orchestrator.infrastructure.persistence.jpa.OutboxEventJpaEntity;
import com.voltnet.orchestrator.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Patron Observer (GoF) implementado como Transactional Outbox:
 * convierte el evento de dominio a JSON y lo persiste en outbox_events.
 * Como esto sucede dentro de la misma transaccion JPA que cierra la sesion,
 * o ambos se confirman o ambos se revierten — garantia atomica.
 *
 * Luego {@link OutboxRelayWorker} despacha al broker.
 */
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxDomainEventPublisher(OutboxEventJpaRepository outboxRepo,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void publish(ChargeSessionCompletedEvent event) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento de dominio", e);
        }
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                event.sessionId(),
                "ChargeSessionCompleted",
                RabbitMqConfig.CHARGE_COMPLETED_ROUTING_KEY,
                json,
                Instant.now(clock)
        );
        outboxRepo.save(entity);
    }
}
