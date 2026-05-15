package com.voltnet.orchestrator.infrastructure.persistence.repository;

import com.voltnet.orchestrator.infrastructure.persistence.jpa.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
}
