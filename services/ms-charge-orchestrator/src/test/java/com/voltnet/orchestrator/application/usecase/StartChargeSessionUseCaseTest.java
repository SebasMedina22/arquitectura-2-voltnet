package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.exception.ChargeRuleViolationException;
import com.voltnet.orchestrator.domain.factory.ChargeSessionFactory;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.StationLoadSnapshot;
import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.policy.GridCapacityPolicy;
import com.voltnet.orchestrator.domain.policy.UserSolvencyPolicy;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;
import com.voltnet.orchestrator.domain.port.out.GridLoadPort;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StartChargeSessionUseCaseTest {

    private Map<String, User> users;
    private Map<String, ChargeSession> sessions;
    private double simulatedKw;
    private boolean overloaded;

    private StartChargeSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        users = new HashMap<>();
        sessions = new HashMap<>();
        simulatedKw = 50.0;
        overloaded = false;

        GridLoadPort gridPort = id -> new StationLoadSnapshot(id, BigDecimal.valueOf(simulatedKw), overloaded);
        UserRepository userRepo = new UserRepository() {
            @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(users.get(id.value())); }
            @Override public void save(User user) { users.put(user.id().value(), user); }
        };
        ChargeSessionRepository sessionRepo = new ChargeSessionRepository() {
            @Override public void save(ChargeSession s) { sessions.put(s.id().value(), s); }
            @Override public Optional<ChargeSession> findById(SessionId id) { return Optional.ofNullable(sessions.get(id.value())); }
            @Override public List<ChargeSession> findByUserId(UserId userId) {
                return sessions.values().stream().filter(s -> s.userId().equals(userId)).toList();
            }
        };

        Clock fixed = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneId.of("UTC"));
        useCase = new StartChargeSessionUseCase(
                List.of(new GridCapacityPolicy(gridPort, 100.0), new UserSolvencyPolicy(userRepo, 30)),
                new ChargeSessionFactory(fixed),
                sessionRepo
        );

        users.put("USR-001", new User(new UserId("USR-001"), 0, true, Instant.now()));
        users.put("USR-003", new User(new UserId("USR-003"), 45, true, Instant.now()));
    }

    @Test
    void inicia_caminoFeliz() {
        ChargeSession s = useCase.execute(new UserId("USR-001"), new StationId("STN-001"));
        assertNotNull(s.id());
        assertEquals(1, sessions.size());
    }

    @Test
    void rechaza_porR1_cuandoOverloaded() {
        overloaded = true;
        simulatedKw = 130.0;
        ChargeRuleViolationException ex = assertThrows(ChargeRuleViolationException.class,
                () -> useCase.execute(new UserId("USR-001"), new StationId("STN-002")));
        assertEquals("R1_GRID_OVERLOADED", ex.ruleCode());
        assertTrue(sessions.isEmpty());
    }

    @Test
    void rechaza_porR2_cuandoDeudaMayor30Dias() {
        ChargeRuleViolationException ex = assertThrows(ChargeRuleViolationException.class,
                () -> useCase.execute(new UserId("USR-003"), new StationId("STN-001")));
        assertEquals("R2_USER_NOT_SOLVENT", ex.ruleCode());
        assertTrue(sessions.isEmpty());
    }
}
