package com.voltnet.orchestrator.domain.policy;

import com.voltnet.orchestrator.domain.exception.ChargeRuleViolationException;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserSolvencyPolicyTest {

    private final Map<String, User> store = new HashMap<>();

    private final UserRepository repo = new UserRepository() {
        @Override public Optional<User> findById(UserId id) {
            return Optional.ofNullable(store.get(id.value()));
        }
        @Override public void save(User user) { store.put(user.id().value(), user); }
    };

    private static final StationId ANY_STATION = new StationId("STN-001");

    @Test
    void permite_usuarioSinDeudaConMedioActivo() {
        repo.save(new User(new UserId("USR-001"), 0, true, Instant.now()));
        UserSolvencyPolicy policy = new UserSolvencyPolicy(repo, 30);
        assertDoesNotThrow(() -> policy.check(new UserId("USR-001"), ANY_STATION));
    }

    @Test
    void rechaza_usuarioSinMedioDePago() {
        repo.save(new User(new UserId("USR-002"), 0, false, Instant.now()));
        UserSolvencyPolicy policy = new UserSolvencyPolicy(repo, 30);
        ChargeRuleViolationException ex = assertThrows(ChargeRuleViolationException.class,
                () -> policy.check(new UserId("USR-002"), ANY_STATION));
        assertEquals("R2_USER_NOT_SOLVENT", ex.ruleCode());
    }

    @Test
    void rechaza_deudaMayorA30Dias() {
        repo.save(new User(new UserId("USR-003"), 45, true, Instant.now()));
        UserSolvencyPolicy policy = new UserSolvencyPolicy(repo, 30);
        assertThrows(ChargeRuleViolationException.class,
                () -> policy.check(new UserId("USR-003"), ANY_STATION));
    }

    @Test
    void rechaza_usuarioDesconocido_porDefecto() {
        UserSolvencyPolicy policy = new UserSolvencyPolicy(repo, 30);
        assertThrows(ChargeRuleViolationException.class,
                () -> policy.check(new UserId("USR-XYZ"), ANY_STATION));
    }
}
