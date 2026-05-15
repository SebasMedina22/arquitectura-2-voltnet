package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.orchestrator.domain.exception.SessionNotFoundException;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.Kwh;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;
import com.voltnet.orchestrator.domain.port.out.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StopChargeSessionUseCaseTest {

    @Test
    void cierra_yPublicaEvento() {
        Map<String, ChargeSession> store = new HashMap<>();
        ChargeSession session = new ChargeSession(SessionId.generate(),
                new UserId("USR-001"), new StationId("STN-001"), Instant.now());
        store.put(session.id().value(), session);

        List<ChargeSessionCompletedEvent> published = new ArrayList<>();
        DomainEventPublisher publisher = published::add;

        ChargeSessionRepository repo = new ChargeSessionRepository() {
            @Override public void save(ChargeSession s) { store.put(s.id().value(), s); }
            @Override public Optional<ChargeSession> findById(SessionId id) { return Optional.ofNullable(store.get(id.value())); }
            @Override public List<ChargeSession> findByUserId(UserId u) { return List.of(); }
        };

        StopChargeSessionUseCase uc = new StopChargeSessionUseCase(repo, publisher,
                Clock.fixed(Instant.parse("2026-05-15T11:00:00Z"), ZoneId.of("UTC")));

        ChargeSession closed = uc.execute(session.id(), Kwh.of(15.5));

        assertTrue(closed.isCompleted());
        assertEquals(1, published.size());
        assertEquals(session.id().value(), published.get(0).sessionId());
        assertEquals(15.5, published.get(0).kwhConsumed());
    }

    @Test
    void falla_siSesionNoExiste() {
        ChargeSessionRepository repo = new ChargeSessionRepository() {
            @Override public void save(ChargeSession s) {}
            @Override public Optional<ChargeSession> findById(SessionId id) { return Optional.empty(); }
            @Override public List<ChargeSession> findByUserId(UserId u) { return List.of(); }
        };
        StopChargeSessionUseCase uc = new StopChargeSessionUseCase(repo, e -> {}, Clock.systemUTC());
        assertThrows(SessionNotFoundException.class,
                () -> uc.execute(new SessionId("missing-id"), Kwh.of(1)));
    }
}
