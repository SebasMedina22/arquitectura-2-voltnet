package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.event.UserDebtUpdatedEvent;
import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateUserDebtUseCaseTest {

    private final Map<String, User> store = new HashMap<>();
    private final UserRepository repo = new UserRepository() {
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(store.get(id.value())); }
        @Override public void save(User user) { store.put(user.id().value(), user); }
    };

    @Test
    void crea_usuarioNuevoAlRecibirEvento() {
        UpdateUserDebtUseCase uc = new UpdateUserDebtUseCase(repo);
        uc.execute(new UserDebtUpdatedEvent("USR-009", 5, true, Instant.parse("2026-05-15T10:00:00Z")));
        assertEquals(5, store.get("USR-009").overdueDays());
        assertTrue(store.get("USR-009").hasActivePaymentMethod());
    }

    @Test
    void actualiza_proyeccionExistente() {
        store.put("USR-001", new User(new UserId("USR-001"), 0, true, Instant.parse("2026-05-15T09:00:00Z")));
        UpdateUserDebtUseCase uc = new UpdateUserDebtUseCase(repo);
        uc.execute(new UserDebtUpdatedEvent("USR-001", 45, true, Instant.parse("2026-05-15T10:00:00Z")));
        assertEquals(45, store.get("USR-001").overdueDays());
    }

    @Test
    void ignora_eventoMasViejo_idempotencia() {
        store.put("USR-001", new User(new UserId("USR-001"), 10, true, Instant.parse("2026-05-15T12:00:00Z")));
        UpdateUserDebtUseCase uc = new UpdateUserDebtUseCase(repo);
        uc.execute(new UserDebtUpdatedEvent("USR-001", 50, true, Instant.parse("2026-05-15T09:00:00Z")));
        assertEquals(10, store.get("USR-001").overdueDays(), "evento viejo no debe sobrescribir");
    }
}
