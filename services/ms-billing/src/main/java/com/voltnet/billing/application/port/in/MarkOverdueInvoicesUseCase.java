package com.voltnet.billing.application.port.in;

public interface MarkOverdueInvoicesUseCase {

    /**
     * Recorre las facturas PENDING cuya dueAt ya paso y las marca como OVERDUE.
     * Por cada usuario afectado publica UserDebtUpdatedEvent.
     * Devuelve el numero de facturas marcadas.
     */
    int markAllOverdue();
}
