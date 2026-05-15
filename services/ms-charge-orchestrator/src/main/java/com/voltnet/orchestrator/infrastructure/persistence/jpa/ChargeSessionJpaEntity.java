package com.voltnet.orchestrator.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

import com.voltnet.orchestrator.domain.model.ChargeSessionStatus;

@Entity
@Table(name = "charge_sessions")
public class ChargeSessionJpaEntity {

    @Id
    @Column(name = "id", length = 64, columnDefinition = "VARCHAR(64)")
    private String id;

    @Column(name = "user_id", length = 32, nullable = false, columnDefinition = "VARCHAR(32)")
    private String userId;

    @Column(name = "station_id", length = 32, nullable = false, columnDefinition = "VARCHAR(32)")
    private String stationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false, columnDefinition = "VARCHAR(16)")
    private ChargeSessionStatus status;

    @Column(name = "kwh_consumed", nullable = false, precision = 12, scale = 3)
    private BigDecimal kwhConsumed;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ChargeSessionJpaEntity() {}

    public ChargeSessionJpaEntity(String id, String userId, String stationId,
                                  ChargeSessionStatus status, BigDecimal kwhConsumed,
                                  Instant startedAt, Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.stationId = stationId;
        this.status = status;
        this.kwhConsumed = kwhConsumed;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getStationId() { return stationId; }
    public ChargeSessionStatus getStatus() { return status; }
    public BigDecimal getKwhConsumed() { return kwhConsumed; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setStatus(ChargeSessionStatus status) { this.status = status; }
    public void setKwhConsumed(BigDecimal kwhConsumed) { this.kwhConsumed = kwhConsumed; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
