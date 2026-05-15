package com.voltnet.orchestrator.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 32, columnDefinition = "VARCHAR(32)")
    private String id;

    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    @Column(name = "has_active_payment_method", nullable = false)
    private boolean hasActivePaymentMethod;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    protected UserJpaEntity() {}

    public UserJpaEntity(String id, int overdueDays, boolean hasActivePaymentMethod, Instant lastUpdatedAt) {
        this.id = id;
        this.overdueDays = overdueDays;
        this.hasActivePaymentMethod = hasActivePaymentMethod;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getId() { return id; }
    public int getOverdueDays() { return overdueDays; }
    public boolean isHasActivePaymentMethod() { return hasActivePaymentMethod; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }

    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
    public void setHasActivePaymentMethod(boolean v) { this.hasActivePaymentMethod = v; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
