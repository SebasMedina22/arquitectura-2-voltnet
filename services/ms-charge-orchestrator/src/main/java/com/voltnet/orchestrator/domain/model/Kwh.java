package com.voltnet.orchestrator.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object: energia consumida en kilovatios-hora.
 * Invariante defensiva: no negativo. Precision fija a 3 decimales (Wh).
 */
public final class Kwh {

    private final BigDecimal value;

    public Kwh(BigDecimal value) {
        Objects.requireNonNull(value, "Kwh no puede ser null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Kwh no puede ser negativo: " + value);
        }
        this.value = value.setScale(3, RoundingMode.HALF_UP);
    }

    public static Kwh of(double v) {
        return new Kwh(BigDecimal.valueOf(v));
    }

    public static Kwh zero() {
        return new Kwh(BigDecimal.ZERO);
    }

    public BigDecimal value() {
        return value;
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kwh other)) return false;
        return value.compareTo(other.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString() + " kWh";
    }
}
