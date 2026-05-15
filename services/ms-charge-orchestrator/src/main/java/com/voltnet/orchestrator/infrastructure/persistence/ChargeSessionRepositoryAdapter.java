package com.voltnet.orchestrator.infrastructure.persistence;

import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.Kwh;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;
import com.voltnet.orchestrator.infrastructure.persistence.jpa.ChargeSessionJpaEntity;
import com.voltnet.orchestrator.infrastructure.persistence.repository.ChargeSessionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Patron Adapter (GoF) lado salida: implementa el puerto del dominio usando
 * Spring Data JPA. El dominio no conoce JPA.
 */
@Component
public class ChargeSessionRepositoryAdapter implements ChargeSessionRepository {

    private final ChargeSessionJpaRepository jpa;

    public ChargeSessionRepositoryAdapter(ChargeSessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ChargeSession session) {
        ChargeSessionJpaEntity entity = new ChargeSessionJpaEntity(
                session.id().value(),
                session.userId().value(),
                session.stationId().value(),
                session.status(),
                session.kwhConsumed().value(),
                session.startedAt(),
                session.completedAt()
        );
        jpa.save(entity);
    }

    @Override
    public Optional<ChargeSession> findById(SessionId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ChargeSession> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(this::toDomain).toList();
    }

    private ChargeSession toDomain(ChargeSessionJpaEntity e) {
        return ChargeSession.rehydrate(
                new SessionId(e.getId()),
                new UserId(e.getUserId()),
                new StationId(e.getStationId()),
                e.getStartedAt(),
                e.getStatus(),
                new Kwh(e.getKwhConsumed()),
                e.getCompletedAt()
        );
    }
}
