package com.voltnet.gridload.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object: identificador de estacion.
 * RG-2: formato STN-XXX (3 digitos), no vacio.
 */
public final class StationId {

    private static final Pattern PATTERN = Pattern.compile("^STN-\\d{3}$");

    private final String value;

    private StationId(String value) {
        this.value = value;
    }

    public static StationId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("StationId no puede ser vacio");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "StationId no cumple formato STN-XXX (3 digitos): " + value);
        }
        return new StationId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StationId that)) return false;
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
