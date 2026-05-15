package com.voltnet.orchestrator.domain.port.out;

import com.voltnet.orchestrator.domain.event.ChargeSessionCompletedEvent;

/**
 * Patron Observer (GoF): puerto de salida para publicar eventos de dominio.
 * Implementacion por defecto: outbox (escritura transaccional a tabla local)
 * + worker scheduleado que despacha a RabbitMQ. Esto sostiene R3.
 */
public interface DomainEventPublisher {
    void publish(ChargeSessionCompletedEvent event);
}
