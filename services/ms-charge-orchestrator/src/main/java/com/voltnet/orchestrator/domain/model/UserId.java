package com.voltnet.orchestrator.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: identificador de usuario. Invariante defensiva: formato USR-XXX...
 * (alineado con el seed y los eventos UserDebtUpdated emitidos por MS-Billing).
 */
public final class UserId {

    private static final Pattern PATTERN = Pattern.compile("^USR-[A-Za-z0-9]{1,16}$");

    private final String value;

    public UserId(String value) {
        Objects.requireNonNull(value, "UserId no puede ser null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("UserId invalido: " + value + " (esperado USR-XXX)");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId other)) return false;
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
