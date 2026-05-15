package com.voltnet.orchestrator.application.usecase;

import com.voltnet.orchestrator.domain.model.ChargeSession;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.ChargeSessionRepository;

import java.util.List;

public class ListUserSessionsUseCase {

    private final ChargeSessionRepository repository;

    public ListUserSessionsUseCase(ChargeSessionRepository repository) {
        this.repository = repository;
    }

    public List<ChargeSession> execute(UserId userId) {
        return repository.findByUserId(userId);
    }
}
