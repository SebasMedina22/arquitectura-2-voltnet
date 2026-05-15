package com.voltnet.orchestrator.domain.factory;

import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;

import java.time.Clock;
import java.time.Instant;

/**
 * Patron Factory (GoF): centraliza la construccion de ChargeSession
 * generando el SessionId y fijando el startedAt con un Clock inyectable
 * (testeable). Mantiene el caso de uso libre de detalles de creacion.
 */
public class ChargeSessionFactory {

    private final Clock clock;

    public ChargeSessionFactory(Clock clock) {
        this.clock = clock;
    }

    public ChargeSession create(UserId userId, StationId stationId) {
        SessionId id = SessionId.generate();
        Instant now = Instant.now(clock);
        return new ChargeSession(id, userId, stationId, now);
    }
}
