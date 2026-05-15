package com.voltnet.billing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object: monto monetario con moneda.
 * Invariante defensiva: amount no negativo; currency obligatoria (ISO 4217, 3 letras).
 */
public final class Money {

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Money.amount no puede ser null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money.amount no puede ser negativo: " + amount);
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Money.currency es obligatoria");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException(
                    "Money.currency debe seguir ISO 4217 (3 letras): " + currency);
        }
        return new Money(amount.setScale(2, RoundingMode.HALF_UP), currency.toUpperCase());
    }

    public static Money ofKwhTimesRate(double kwh, BigDecimal ratePerKwh, String currency) {
        if (kwh < 0) {
            throw new IllegalArgumentException("kWh no puede ser negativo: " + kwh);
        }
        BigDecimal amount = ratePerKwh.multiply(BigDecimal.valueOf(kwh));
        return of(amount, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
