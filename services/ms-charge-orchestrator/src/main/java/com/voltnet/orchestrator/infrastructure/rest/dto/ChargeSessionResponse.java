package com.voltnet.orchestrator.infrastructure.rest.dto;

import com.voltnet.orchestrator.domain.model.ChargeSession;

import java.math.BigDecimal;
import java.time.Instant;

public record ChargeSessionResponse(
        String sessionId,
        String userId,
        String stationId,
        String status,
        BigDecimal kwhConsumed,
        Instant startedAt,
        Instant completedAt
) {
    public static ChargeSessionResponse from(ChargeSession s) {
        return new ChargeSessionResponse(
                s.id().value(),
                s.userId().value(),
                s.stationId().value(),
                s.status().name(),
                s.kwhConsumed().value(),
                s.startedAt(),
                s.completedAt()
        );
    }
}
