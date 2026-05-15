package com.voltnet.billing.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Topologia AMQP:
 *  - Exchange voltnet.charge.events (topic) publica eventos del Orchestrator.
 *    Billing escucha la routing key "charge.session.completed" en una cola
 *    dedicada con DLQ asociada.
 *  - Exchange voltnet.billing.events (topic) lo usa Billing para publicar
 *    UserDebtUpdated.
 */
@Configuration
public class RabbitMqConfig {

    public static final String CHARGE_EXCHANGE  = "voltnet.charge.events";
    public static final String BILLING_EXCHANGE = "voltnet.billing.events";

    public static final String CHARGE_COMPLETED_ROUTING_KEY = "charge.session.completed";
    public static final String USER_DEBT_UPDATED_ROUTING_KEY = "user.debt.updated";

    public static final String BILLING_COMPLETED_QUEUE = "billing.charge-session-completed.q";
    public static final String BILLING_COMPLETED_DLQ   = "billing.charge-session-completed.dlq";
    public static final String DLX_EXCHANGE            = "voltnet.billing.dlx";

    @Bean
    public TopicExchange chargeExchange() {
        return new TopicExchange(CHARGE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue billingCompletedQueue() {
        return QueueBuilder.durable(BILLING_COMPLETED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BILLING_COMPLETED_DLQ)
                .build();
    }

    @Bean
    public Queue billingCompletedDlq() {
        return QueueBuilder.durable(BILLING_COMPLETED_DLQ).build();
    }

    @Bean
    public Binding billingCompletedBinding(Queue billingCompletedQueue, TopicExchange chargeExchange) {
        return BindingBuilder.bind(billingCompletedQueue)
                .to(chargeExchange)
                .with(CHARGE_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding billingDlqBinding(Queue billingCompletedDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(billingCompletedDlq)
                .to(deadLetterExchange)
                .with(BILLING_COMPLETED_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
}
