package com.voltnet.billing.infrastructure.messaging;

import com.voltnet.billing.domain.event.UserDebtUpdatedEvent;
import com.voltnet.billing.domain.port.out.UserDebtEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Patron Adapter (GoF) saliente: traduce el evento de dominio UserDebtUpdated
 * a una publicacion AMQP en voltnet.billing.events con routing key
 * user.debt.updated. Publisher confirms quedan habilitados via RabbitMqConfig.
 */
@Component
public class RabbitUserDebtUpdatedPublisher implements UserDebtEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitUserDebtUpdatedPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitUserDebtUpdatedPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(UserDebtUpdatedEvent event) {
        log.info("Publicando UserDebtUpdated userId={} overdueDays={}",
                event.userId(), event.overdueDays());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.BILLING_EXCHANGE,
                RabbitMqConfig.USER_DEBT_UPDATED_ROUTING_KEY,
                event
        );
    }
}
