package com.voltnet.orchestrator.application.config;

import com.voltnet.orchestrator.application.usecase.GetChargeSessionUseCase;
import com.voltnet.orchestrator.application.usecase.ListUserSessionsUseCase;
import com.voltnet.orchestrator.application.usecase.StartChargeSessionUseCase;
import com.voltnet.orchestrator.application.usecase.StopChargeSessionUseCase;
import com.voltnet.orchestrator.application.usecase.UpdateUserDebtUseCase;
import com.voltnet.orchestrator.domain.factory.ChargeSessionFactory;
import com.voltnet.orchestrator.domain.policy.ChargeStartPolicy;
import com.voltnet.orchestrator.domain.policy.GridCapacityPolicy;
import com.voltnet.orchestrator.domain.policy.UserSolvencyPolicy;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;
import com.voltnet.orchestrator.domain.port.out.DomainEventPublisher;
import com.voltnet.orchestrator.domain.port.out.GridLoadPort;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Composicion: instancia los casos de uso y las politicas como @Bean.
 * El dominio sigue siendo agnostico a Spring; solo aqui (capa de aplicacion)
 * se conecta el cableado.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ChargeSessionFactory chargeSessionFactory(Clock clock) {
        return new ChargeSessionFactory(clock);
    }

    @Bean
    public GridCapacityPolicy gridCapacityPolicy(
            GridLoadPort gridLoadPort,
            @Value("${orchestrator.rules.grid-overload-threshold-kw:100.0}") double thresholdKw) {
        return new GridCapacityPolicy(gridLoadPort, thresholdKw);
    }

    @Bean
    public UserSolvencyPolicy userSolvencyPolicy(
            UserRepository userRepository,
            @Value("${orchestrator.rules.user-overdue-blocking-days:30}") int overdueBlockingDays) {
        return new UserSolvencyPolicy(userRepository, overdueBlockingDays);
    }

    @Bean
    public StartChargeSessionUseCase startChargeSessionUseCase(
            GridCapacityPolicy gridCapacityPolicy,
            UserSolvencyPolicy userSolvencyPolicy,
            ChargeSessionFactory factory,
            ChargeSessionRepository repository) {
        List<ChargeStartPolicy> policies = List.of(gridCapacityPolicy, userSolvencyPolicy);
        return new StartChargeSessionUseCase(policies, factory, repository);
    }

    @Bean
    public StopChargeSessionUseCase stopChargeSessionUseCase(
            ChargeSessionRepository repository,
            DomainEventPublisher publisher,
            Clock clock) {
        return new StopChargeSessionUseCase(repository, publisher, clock);
    }

    @Bean
    public GetChargeSessionUseCase getChargeSessionUseCase(ChargeSessionRepository repository) {
        return new GetChargeSessionUseCase(repository);
    }

    @Bean
    public ListUserSessionsUseCase listUserSessionsUseCase(ChargeSessionRepository repository) {
        return new ListUserSessionsUseCase(repository);
    }

    @Bean
    public UpdateUserDebtUseCase updateUserDebtUseCase(UserRepository userRepository) {
        return new UpdateUserDebtUseCase(userRepository);
    }
}
