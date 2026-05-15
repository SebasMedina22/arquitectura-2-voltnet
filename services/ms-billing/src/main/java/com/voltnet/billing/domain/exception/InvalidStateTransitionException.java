package com.voltnet.billing.domain.exception;

import com.voltnet.billing.domain.model.InvoiceStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(InvoiceStatus from, InvoiceStatus to) {
        super("Transicion invalida: " + from + " -> " + to);
    }
}
