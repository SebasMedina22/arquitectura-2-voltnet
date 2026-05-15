package com.voltnet.billing.domain.exception;

/**
 * Lanzada cuando se intenta crear una factura para una sessionId que ya fue facturada.
 * Es el cerrojo a nivel de aplicacion; el cerrojo a nivel de DB es UNIQUE(session_id).
 * Capturada por el consumer AMQP para hacer ACK silencioso del mensaje duplicado.
 */
public class DuplicateInvoiceException extends RuntimeException {

    public DuplicateInvoiceException(String sessionId) {
        super("Ya existe una factura para session_id=" + sessionId);
    }
}
