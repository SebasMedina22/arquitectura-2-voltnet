package com.voltnet.gridload.domain.model;

import java.util.Objects;

/**
 * Value object: kilovatios de carga.
 * RG-1: no negativos, no NaN, no Infinity.
 */
public final class Kw {

    private final double value;

    private Kw(double value) {
        this.value = value;
    }

    public static Kw of(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("Kw no puede ser NaN");
        }
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("Kw no puede ser infinito");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException("Kw no puede ser negativo: " + value);
        }
        return new Kw(value);
    }

    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Kw kw)) return false;
        return Double.compare(kw.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value + " kW";
    }
}
