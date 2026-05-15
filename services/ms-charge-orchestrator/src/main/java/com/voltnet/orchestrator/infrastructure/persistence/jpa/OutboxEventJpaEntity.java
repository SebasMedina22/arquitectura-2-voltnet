package com.voltnet.orchestrator.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Tabla outbox: cada evento de dominio que el Orchestrator quiere publicar
 * se persiste primero aqui dentro de la misma transaccion que cambio el estado.
 * Un worker scheduleado lo despacha luego al broker (Observer GoF), garantizando
 * la propiedad "al-menos-una-vez" incluso si RabbitMQ esta caido.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", length = 64, nullable = false, columnDefinition = "VARCHAR(64)")
    private String aggregateId;

    @Column(name = "event_type", length = 64, nullable = false, columnDefinition = "VARCHAR(64)")
    private String eventType;

    @Column(name = "routing_key", length = 128, nullable = false, columnDefinition = "VARCHAR(128)")
    private String routingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    protected OutboxEventJpaEntity() {}

    public OutboxEventJpaEntity(String aggregateId, String eventType, String routingKey,
                                String payload, Instant createdAt) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
        this.attempts = 0;
    }

    public Long getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }

    public void markPublished(Instant at) {
        this.publishedAt = at;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
