package com.voltnet.billing.application.service;

import com.voltnet.billing.application.port.in.MarkOverdueInvoicesUseCase;
import com.voltnet.billing.domain.event.UserDebtUpdatedEvent;
import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.UserDebt;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.InvoiceRepository;
import com.voltnet.billing.domain.port.out.UserDebtEventPublisher;
import com.voltnet.billing.domain.port.out.UserDebtRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Caso de uso: marcar como OVERDUE las facturas PENDING vencidas y publicar
 * UserDebtUpdated por cada usuario afectado.
 *
 * Lo invoca el scheduled job de infraestructura. La logica de seleccion de
 * candidatos vive en el repositorio (puerto out), pero la decision de
 * transicion de estado vive aqui + en el agregado Invoice.
 */
public class MarkOverdueInvoicesService implements MarkOverdueInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;
    private final UserDebtRepository userDebtRepository;
    private final UserDebtEventPublisher publisher;
    private final Clock clock;

    public MarkOverdueInvoicesService(InvoiceRepository invoiceRepository,
                                      UserDebtRepository userDebtRepository,
                                      UserDebtEventPublisher publisher,
                                      Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.userDebtRepository = userDebtRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public int markAllOverdue() {
        Instant now = Instant.now(clock);
        List<Invoice> candidates = invoiceRepository.findOverdueCandidates(now);
        int count = 0;
        for (Invoice invoice : candidates) {
            if (!invoice.isOverdueAt(now)) {
                continue;
            }
            invoice.markOverdue(now);
            invoiceRepository.save(invoice);
            UserDebt debt = recalculateDebt(invoice.userId(), now);
            userDebtRepository.save(debt);
            publisher.publish(UserDebtUpdatedEvent.fromDomain(debt));
            count++;
        }
        return count;
    }

    private UserDebt recalculateDebt(UserId userId, Instant now) {
        List<Invoice> userInvoices = invoiceRepository.findByUserId(userId);
        int maxOverdueDays = userInvoices.stream()
                .filter(i -> i.status() == com.voltnet.billing.domain.model.InvoiceStatus.OVERDUE)
                .map(i -> (int) Duration.between(i.dueAt(), now).toDays())
                .max(Integer::compareTo)
                .orElse(0);
        return UserDebt.of(userId, maxOverdueDays, true, now);
    }
}
