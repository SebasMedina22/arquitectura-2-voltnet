package com.voltnet.orchestrator.infrastructure.messaging;

import com.voltnet.orchestrator.infrastructure.persistence.jpa.OutboxEventJpaEntity;
import com.voltnet.orchestrator.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Worker scheduleado que despacha eventos pendientes de la tabla outbox
 * al broker AMQP. Si la publicacion falla (broker caido), el registro
 * queda sin publishedAt y se reintenta en el siguiente tick. Asi se
 * sostiene R3: la sesion se cierra siempre, el evento sale cuando
 * el broker vuelva.
 */
@Component
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxEventJpaRepository repo;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final int batchSize;

    public OutboxRelayWorker(OutboxEventJpaRepository repo,
                             RabbitTemplate rabbitTemplate,
                             Clock clock,
                             @Value("${orchestrator.outbox.batch-size:50}") int batchSize) {
        this.repo = repo;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${orchestrator.outbox.poll-interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEventJpaEntity> pending = repo.findPending(PageRequest.of(0, batchSize));
        if (pending.isEmpty()) return;

        for (OutboxEventJpaEntity e : pending) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.CHARGE_EXCHANGE,
                        e.getRoutingKey(),
                        e.getPayload()
                );
                e.markPublished(Instant.now(clock));
                repo.save(e);
            } catch (AmqpException ex) {
                e.incrementAttempts();
                repo.save(e);
                log.warn("Outbox: fallo al publicar id={} type={} (intentos={}). Reintentara.",
                        e.getId(), e.getEventType(), e.getAttempts());
            }
        }
    }
}
