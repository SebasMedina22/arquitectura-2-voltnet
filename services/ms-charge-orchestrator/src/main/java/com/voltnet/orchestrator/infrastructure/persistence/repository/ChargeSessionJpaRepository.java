package com.voltnet.orchestrator.infrastructure.persistence.repository;

import com.voltnet.orchestrator.infrastructure.persistence.jpa.ChargeSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeSessionJpaRepository extends JpaRepository<ChargeSessionJpaEntity, String> {
    List<ChargeSessionJpaEntity> findByUserId(String userId);
}
