package com.voltnet.orchestrator.infrastructure.messaging;

import com.voltnet.orchestrator.application.usecase.UpdateUserDebtUseCase;
import com.voltnet.orchestrator.domain.event.UserDebtUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Patron Adapter (GoF) entrante: traduce un mensaje AMQP del exchange
 * voltnet.billing.events al caso de uso UpdateUserDebt. Si el caso de uso
 * falla, el mensaje cae a la DLQ via la configuracion de la cola.
 */
@Component
public class RabbitUserDebtUpdatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitUserDebtUpdatedConsumer.class);

    private final UpdateUserDebtUseCase useCase;

    public RabbitUserDebtUpdatedConsumer(UpdateUserDebtUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMqConfig.ORCH_USER_DEBT_QUEUE)
    public void onMessage(UserDebtUpdatedEvent event) {
        log.info("UserDebtUpdated recibido userId={} overdueDays={}",
                event.userId(), event.overdueDays());
        useCase.execute(event);
    }
}
