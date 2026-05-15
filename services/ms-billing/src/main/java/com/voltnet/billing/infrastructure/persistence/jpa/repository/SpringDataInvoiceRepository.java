package com.voltnet.billing.infrastructure.persistence.jpa.repository;

import com.voltnet.billing.domain.model.InvoiceStatus;
import com.voltnet.billing.infrastructure.persistence.jpa.entity.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    List<InvoiceJpaEntity> findByUserId(String userId);

    @Query("SELECT i FROM InvoiceJpaEntity i WHERE i.status = :status AND i.dueAt <= :now")
    List<InvoiceJpaEntity> findPendingDueAtOrBefore(@Param("status") InvoiceStatus status,
                                                    @Param("now") Instant now);
}
