package com.voltnet.billing.infrastructure.persistence.jpa.adapter;

import com.voltnet.billing.domain.model.UserDebt;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.UserDebtRepository;
import com.voltnet.billing.infrastructure.persistence.jpa.entity.UserDebtJpaEntity;
import com.voltnet.billing.infrastructure.persistence.jpa.repository.SpringDataUserDebtRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserDebtRepositoryAdapter implements UserDebtRepository {

    private final SpringDataUserDebtRepository jpa;

    public JpaUserDebtRepositoryAdapter(SpringDataUserDebtRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserDebt> findByUserId(UserId userId) {
        return jpa.findById(userId.value()).map(this::toDomain);
    }

    @Override
    public UserDebt save(UserDebt debt) {
        UserDebtJpaEntity entity = jpa.findById(debt.userId().value())
                .orElseGet(() -> new UserDebtJpaEntity(
                        debt.userId().value(),
                        debt.overdueDays(),
                        debt.hasActivePaymentMethod(),
                        debt.lastUpdatedAt()
                ));
        entity.setOverdueDays(debt.overdueDays());
        entity.setHasActivePaymentMethod(debt.hasActivePaymentMethod());
        entity.setLastUpdatedAt(debt.lastUpdatedAt());
        return toDomain(jpa.save(entity));
    }

    private UserDebt toDomain(UserDebtJpaEntity e) {
        return UserDebt.of(
                UserId.of(e.getUserId()),
                e.getOverdueDays(),
                e.isHasActivePaymentMethod(),
                e.getLastUpdatedAt()
        );
    }
}
