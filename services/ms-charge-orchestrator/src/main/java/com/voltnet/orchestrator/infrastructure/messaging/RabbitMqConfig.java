package com.voltnet.orchestrator.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

/**
 * Topologia AMQP del lado Orchestrator:
 *  - voltnet.charge.events (topic, declarado tambien por Billing) — publica
 *    ChargeSessionCompleted con routing key charge.session.completed.
 *  - voltnet.billing.events (topic) — Orchestrator se suscribe a la routing
 *    key user.debt.updated en su propia cola con DLQ.
 *
 * Los nombres deben coincidir con los de MS-Billing (ver
 * RabbitMqConfig de billing) para garantizar el binding correcto.
 */
@Configuration
public class RabbitMqConfig {

    public static final String CHARGE_EXCHANGE  = "voltnet.charge.events";
    public static final String BILLING_EXCHANGE = "voltnet.billing.events";

    public static final String CHARGE_COMPLETED_ROUTING_KEY = "charge.session.completed";
    public static final String USER_DEBT_UPDATED_ROUTING_KEY = "user.debt.updated";

    public static final String ORCH_USER_DEBT_QUEUE = "orchestrator.user-debt-updated.q";
    public static final String ORCH_USER_DEBT_DLQ   = "orchestrator.user-debt-updated.dlq";
    public static final String DLX_EXCHANGE         = "voltnet.orchestrator.dlx";

    @Bean
    public TopicExchange chargeExchange() {
        return new TopicExchange(CHARGE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orchestratorDlx() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orchestratorUserDebtQueue() {
        return QueueBuilder.durable(ORCH_USER_DEBT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORCH_USER_DEBT_DLQ)
                .build();
    }

    @Bean
    public Queue orchestratorUserDebtDlq() {
        return QueueBuilder.durable(ORCH_USER_DEBT_DLQ).build();
    }

    @Bean
    public Binding orchestratorUserDebtBinding(Queue orchestratorUserDebtQueue, TopicExchange billingExchange) {
        return BindingBuilder.bind(orchestratorUserDebtQueue)
                .to(billingExchange)
                .with(USER_DEBT_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding orchestratorUserDebtDlqBinding(Queue orchestratorUserDebtDlq, TopicExchange orchestratorDlx) {
        return BindingBuilder.bind(orchestratorUserDebtDlq)
                .to(orchestratorDlx)
                .with(ORCH_USER_DEBT_DLQ);
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
