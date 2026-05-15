package com.voltnet.billing.application;

import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.InvoiceStatus;
import com.voltnet.billing.domain.model.SessionId;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.InvoiceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryInvoiceRepository implements InvoiceRepository {

    private final Map<UUID, Invoice> byId = new HashMap<>();

    @Override
    public Invoice save(Invoice invoice) {
        byId.put(invoice.id(), invoice);
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Invoice> findBySessionId(SessionId sessionId) {
        return byId.values().stream()
                .filter(i -> i.sessionId().equals(sessionId))
                .findFirst();
    }

    @Override
    public boolean existsBySessionId(SessionId sessionId) {
        return findBySessionId(sessionId).isPresent();
    }

    @Override
    public List<Invoice> findOverdueCandidates(Instant now) {
        List<Invoice> result = new ArrayList<>();
        for (Invoice i : byId.values()) {
            if (i.status() == InvoiceStatus.PENDING && !now.isBefore(i.dueAt())) {
                result.add(i);
            }
        }
        return result;
    }

    @Override
    public List<Invoice> findByUserId(UserId userId) {
        return byId.values().stream()
                .filter(i -> i.userId().equals(userId))
                .toList();
    }
}
