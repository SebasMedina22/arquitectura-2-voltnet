package com.voltnet.billing.domain.port.out;

import com.voltnet.billing.domain.event.UserDebtUpdatedEvent;

/**
 * Puerto de salida. La implementacion en infraestructura (RabbitUserDebtUpdatedPublisher)
 * traduce el evento de dominio a un mensaje AMQP.
 */
public interface UserDebtEventPublisher {

    void publish(UserDebtUpdatedEvent event);
}
