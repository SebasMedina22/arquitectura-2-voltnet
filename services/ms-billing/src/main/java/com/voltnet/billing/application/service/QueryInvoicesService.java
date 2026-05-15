package com.voltnet.billing.application.service;

import com.voltnet.billing.application.port.in.QueryInvoicesUseCase;
import com.voltnet.billing.domain.model.Invoice;
import com.voltnet.billing.domain.model.UserId;
import com.voltnet.billing.domain.port.out.InvoiceRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QueryInvoicesService implements QueryInvoicesUseCase {

    private final InvoiceRepository invoiceRepository;

    public QueryInvoicesService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Override
    public List<Invoice> findByUserId(String rawUserId) {
        return invoiceRepository.findByUserId(UserId.of(rawUserId));
    }
}
