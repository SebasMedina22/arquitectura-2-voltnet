package com.voltnet.orchestrator.infrastructure.persistence;

import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;
import com.voltnet.orchestrator.infrastructure.persistence.jpa.UserJpaEntity;
import com.voltnet.orchestrator.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value())
                .map(e -> new User(new UserId(e.getId()), e.getOverdueDays(),
                        e.isHasActivePaymentMethod(), e.getLastUpdatedAt()));
    }

    @Override
    public void save(User user) {
        jpa.save(new UserJpaEntity(
                user.id().value(),
                user.overdueDays(),
                user.hasActivePaymentMethod(),
                user.lastUpdatedAt()
        ));
    }
}
