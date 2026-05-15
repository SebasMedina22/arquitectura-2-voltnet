package com.voltnet.billing.infrastructure.messaging;

import com.voltnet.billing.application.port.in.ProcessChargeSessionUseCase;
import com.voltnet.billing.domain.event.ChargeSessionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Patron Adapter (GoF) entrante: traduce mensajes AMQP a la invocacion
 * del puerto de aplicacion ProcessChargeSessionUseCase.
 *
 * - Idempotencia: si llega un duplicado, el use case devuelve la factura
 *   existente sin lanzar excepcion (caso normal por reintento del broker).
 * - Si la DB rechaza por UNIQUE constraint en una race condition, la
 *   capturamos como duplicado tambien.
 * - Cualquier otra excepcion la rechazamos sin reencolar -> va a la DLQ.
 */
@Component
public class RabbitChargeSessionCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitChargeSessionCompletedConsumer.class);

    private final ProcessChargeSessionUseCase useCase;

    public RabbitChargeSessionCompletedConsumer(ProcessChargeSessionUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMqConfig.BILLING_COMPLETED_QUEUE)
    public void onMessage(ChargeSessionCompletedEvent event) {
        log.info("Procesando ChargeSessionCompleted sessionId={}", event.sessionId());
        try {
            useCase.process(event);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicado detectado por UNIQUE(session_id)={} - ACK silencioso", event.sessionId());
        } catch (IllegalArgumentException badPayload) {
            log.warn("Payload invalido (sessionId={}): {} - enviando a DLQ", event.sessionId(), badPayload.getMessage());
            throw new AmqpRejectAndDontRequeueException("Payload invalido", badPayload);
        }
    }
}
