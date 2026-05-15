package com.voltnet.orchestrator.domain.exception;

/**
 * Se lanza cuando MS-GridLoad no responde y el circuit breaker abre.
 * Por politica "en duda, rechazar" (decision §2.8 del ROADMAP) se traduce
 * tambien como bloqueo de inicio. Mapeado a HTTP 503.
 */
public class GridLoadUnavailableException extends RuntimeException {

    public GridLoadUnavailableException(String message) {
        super(message);
    }

    public GridLoadUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
