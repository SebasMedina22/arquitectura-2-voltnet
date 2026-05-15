package com.voltnet.orchestrator.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identificador de una sesion de carga. Invariante defensiva:
 * no nulo, no vacio. Formato libre (UUID por defecto via {@link #generate()}).
 */
public final class SessionId {

    private final String value;

    public SessionId(String value) {
        Objects.requireNonNull(value, "SessionId no puede ser null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SessionId no puede ser vacio");
        }
        this.value = value;
    }

    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
