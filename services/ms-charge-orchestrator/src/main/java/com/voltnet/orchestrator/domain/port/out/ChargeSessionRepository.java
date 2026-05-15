package com.voltnet.orchestrator.domain.port.out;

import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface ChargeSessionRepository {
    void save(ChargeSession session);

    Optional<ChargeSession> findById(SessionId id);

    List<ChargeSession> findByUserId(UserId userId);
}
