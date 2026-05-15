package com.voltnet.billing.application.port.in;

import com.voltnet.billing.domain.model.Invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryInvoicesUseCase {

    Optional<Invoice> findById(UUID id);

    List<Invoice> findByUserId(String userId);
}
