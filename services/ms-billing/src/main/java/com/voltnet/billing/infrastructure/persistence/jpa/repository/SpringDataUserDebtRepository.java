package com.voltnet.billing.infrastructure.persistence.jpa.repository;

import com.voltnet.billing.infrastructure.persistence.jpa.entity.UserDebtJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserDebtRepository extends JpaRepository<UserDebtJpaEntity, String> {
}
