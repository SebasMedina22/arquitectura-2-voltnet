package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.exception.SessionNotFoundException;
import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.SessionId;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;

public class GetChargeSessionUseCase {

    private final ChargeSessionRepository repository;

    public GetChargeSessionUseCase(ChargeSessionRepository repository) {
        this.repository = repository;
    }

    public ChargeSession execute(SessionId sessionId) {
        return repository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesion " + sessionId + " no encontrada"));
    }
}
