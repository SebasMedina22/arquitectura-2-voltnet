package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.event.UserDebtUpdatedEvent;
import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;

import java.util.Optional;

/**
 * Caso de uso aplicado por el consumer AMQP de UserDebtUpdated.
 *
 * Mantiene la proyeccion local de solvencia (tabla users en MySQL) que sostiene
 * R2 sin llamadas sincronas a MS-Billing.
 *
 * Idempotencia: si llega un evento mas viejo que el ultimo aplicado, se ignora
 * (last-write-wins por updatedAt). Asi convivimos con reintentos del broker.
 */
public class UpdateUserDebtUseCase {

    private final UserRepository userRepository;

    public UpdateUserDebtUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(UserDebtUpdatedEvent event) {
        UserId userId = new UserId(event.userId());
        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            User u = existing.get();
            if (event.updatedAt().isBefore(u.lastUpdatedAt())) {
                return;
            }
            u.updateDebt(event.overdueDays(), event.hasActivePaymentMethod(), event.updatedAt());
            userRepository.save(u);
        } else {
            userRepository.save(new User(userId, event.overdueDays(),
                    event.hasActivePaymentMethod(), event.updatedAt()));
        }
    }
}
