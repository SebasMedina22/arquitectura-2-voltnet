package com.voltnet.billing.domain.model;

/**
 * Estados de una factura.
 * Transiciones validas (controladas en Invoice):
 *   PENDING -> PAID
 *   PENDING -> OVERDUE
 *   OVERDUE -> PAID
 * Cualquier otra transicion lanza InvalidStateTransitionException.
 */
public enum InvoiceStatus {
    PENDING,
    PAID,
    OVERDUE
}
