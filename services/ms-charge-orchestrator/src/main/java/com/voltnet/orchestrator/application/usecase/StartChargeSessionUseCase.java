package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.factory.ChargeSessionFactory;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.policy.ChargeStartPolicy;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;

import java.util.List;

/**
 * Caso de uso: iniciar sesion de carga.
 *
 * Orquesta R1 + R2 aplicando cada politica del listado inyectado (Strategy GoF).
 * Si alguna lanza ChargeRuleViolationException, no se crea la sesion.
 * Si pasan todas, la factory crea la sesion y se persiste en estado STARTED.
 *
 * Mantiene SRP: este caso de uso NO conoce HTTP, ni MySQL, ni Feign, ni
 * los detalles de cada politica. Solo orquesta.
 */
public class StartChargeSessionUseCase {

    private final List<ChargeStartPolicy> policies;
    private final ChargeSessionFactory factory;
    private final ChargeSessionRepository repository;

    public StartChargeSessionUseCase(List<ChargeStartPolicy> policies,
                                     ChargeSessionFactory factory,
                                     ChargeSessionRepository repository) {
        this.policies = policies;
        this.factory = factory;
        this.repository = repository;
    }

    public ChargeSession execute(UserId userId, StationId stationId) {
        for (ChargeStartPolicy policy : policies) {
            policy.check(userId, stationId);
        }
        ChargeSession session = factory.create(userId, stationId);
        repository.save(session);
        return session;
    }
}
