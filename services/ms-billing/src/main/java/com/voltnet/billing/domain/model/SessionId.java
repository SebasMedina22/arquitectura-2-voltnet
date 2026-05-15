package com.voltnet.billing.domain.model;

import java.util.Objects;

/**
 * Value object: identificador de sesion de carga.
 * Es la clave de idempotencia: una factura por sessionId, garantizado por UNIQUE en DB.
 */
public final class SessionId {

    private final String value;

    private SessionId(String value) {
        this.value = value;
    }

    public static SessionId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SessionId no puede ser vacio");
        }
        return new SessionId(value.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
