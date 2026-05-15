package com.voltnet.billing.domain.port.out;

import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.SessionId;
import com.voltnet.billing.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findBySessionId(SessionId sessionId);

    boolean existsBySessionId(SessionId sessionId);

    List<Invoice> findOverdueCandidates(Instant now);

    List<Invoice> findByUserId(UserId userId);
}
