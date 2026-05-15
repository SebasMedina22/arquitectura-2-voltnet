package com.voltnet.billing.infrastructure.persistence.jpa.adapter;

import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.InvoiceStatus;
import com.voltnet.billing.domain.model.Money;
import com.voltnet.billing.domain.model.SessionId;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.InvoiceRepository;
import com.voltnet.billing.infrastructure.persistence.jpa.entity.InvoiceJpaEntity;
import com.voltnet.billing.infrastructure.persistence.jpa.repository.SpringDataInvoiceRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patron Adapter (GoF): traduce Invoice (dominio) <-> InvoiceJpaEntity (infraestructura).
 * El dominio nunca importa anotaciones JPA; viven solo en la entidad.
 */
@Component
public class JpaInvoiceRepositoryAdapter implements InvoiceRepository {

    private final SpringDataInvoiceRepository jpa;

    public JpaInvoiceRepositoryAdapter(SpringDataInvoiceRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity saved = jpa.save(toJpa(invoice));
        return toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findBySessionId(SessionId sessionId) {
        return jpa.findBySessionId(sessionId.value()).map(this::toDomain);
    }

    @Override
    public boolean existsBySessionId(SessionId sessionId) {
        return jpa.existsBySessionId(sessionId.value());
    }

    @Override
    public List<Invoice> findOverdueCandidates(Instant now) {
        return jpa.findPendingDueAtOrBefore(InvoiceStatus.PENDING, now).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Invoice> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private InvoiceJpaEntity toJpa(Invoice invoice) {
        return new InvoiceJpaEntity(
                invoice.id(),
                invoice.sessionId().value(),
                invoice.userId().value(),
                invoice.amount().amount(),
                invoice.amount().currency(),
                invoice.status(),
                invoice.createdAt(),
                invoice.dueAt(),
                invoice.paidAt()
        );
    }

    private Invoice toDomain(InvoiceJpaEntity e) {
        return Invoice.rehydrate(
                e.getId(),
                SessionId.of(e.getSessionId()),
                UserId.of(e.getUserId()),
                Money.of(e.getAmount(), e.getCurrency()),
                e.getCreatedAt(),
                e.getDueAt(),
                e.getStatus(),
                e.getPaidAt()
        );
    }
}
