package com.voltnet.orchestrator.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: identificador de estacion de carga. Mismo formato que el
 * usado en MS-GridLoad: STN-XXX...
 */
public final class StationId {

    private static final Pattern PATTERN = Pattern.compile("^STN-[A-Za-z0-9]{1,16}$");

    private final String value;

    public StationId(String value) {
        Objects.requireNonNull(value, "StationId no puede ser null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("StationId invalido: " + value + " (esperado STN-XXX)");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StationId other)) return false;
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
