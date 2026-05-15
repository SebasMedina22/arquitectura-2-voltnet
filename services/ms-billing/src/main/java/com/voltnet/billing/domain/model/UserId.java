package com.voltnet.billing.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object: identificador de usuario.
 * Invariante defensiva: formato U-XXX (X alfanumerico), no vacio.
 */
public final class UserId {

    private static final Pattern PATTERN = Pattern.compile("^U-[A-Z0-9]{3,}$");

    private final String value;

    private UserId(String value) {
        this.value = value;
    }

    public static UserId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId no puede ser vacio");
        }
        String upper = value.toUpperCase();
        if (!PATTERN.matcher(upper).matches()) {
            throw new IllegalArgumentException(
                    "UserId no cumple formato U-XXX: " + value);
        }
        return new UserId(upper);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId that)) return false;
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
