package com.voltnet.billing.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_debts")
public class UserDebtJpaEntity {

    @Id
    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "overdue_days", nullable = false)
    private int overdueDays;

    @Column(name = "has_active_payment_method", nullable = false)
    private boolean hasActivePaymentMethod;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    protected UserDebtJpaEntity() {
    }

    public UserDebtJpaEntity(String userId, int overdueDays,
                             boolean hasActivePaymentMethod, Instant lastUpdatedAt) {
        this.userId = userId;
        this.overdueDays = overdueDays;
        this.hasActivePaymentMethod = hasActivePaymentMethod;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getUserId() { return userId; }
    public int getOverdueDays() { return overdueDays; }
    public boolean isHasActivePaymentMethod() { return hasActivePaymentMethod; }
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }

    public void setOverdueDays(int overdueDays) { this.overdueDays = overdueDays; }
    public void setHasActivePaymentMethod(boolean v) { this.hasActivePaymentMethod = v; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}
