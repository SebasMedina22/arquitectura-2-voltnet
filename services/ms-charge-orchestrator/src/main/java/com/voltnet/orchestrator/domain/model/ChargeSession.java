package com.voltnet.orchestrator.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad raiz: sesion de carga de un EV en una estacion.
 * Maquina de estados simple: STARTED -> COMPLETED (no reabrible).
 * Las invariantes (no completar dos veces, kWh no negativo) se cumplen
 * en {@link #complete(Kwh, Instant)} y en el VO {@link Kwh}.
 */
public class ChargeSession {

    private final SessionId id;
    private final UserId userId;
    private final StationId stationId;
    private final Instant startedAt;
    private ChargeSessionStatus status;
    private Kwh kwhConsumed;
    private Instant completedAt;

    public ChargeSession(SessionId id, UserId userId, StationId stationId, Instant startedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.stationId = Objects.requireNonNull(stationId);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.status = ChargeSessionStatus.STARTED;
        this.kwhConsumed = Kwh.zero();
    }

    /** Reconstitucion desde persistencia. */
    public static ChargeSession rehydrate(SessionId id, UserId userId, StationId stationId,
                                          Instant startedAt, ChargeSessionStatus status,
                                          Kwh kwhConsumed, Instant completedAt) {
        ChargeSession s = new ChargeSession(id, userId, stationId, startedAt);
        s.status = status;
        s.kwhConsumed = kwhConsumed != null ? kwhConsumed : Kwh.zero();
        s.completedAt = completedAt;
        return s;
    }

    /**
     * Cierra la sesion registrando el consumo final. Soporta R3:
     * el calculo se hace localmente y luego se publica un evento al outbox
     * para que MS-Billing facture asincronamente.
     */
    public void complete(Kwh kwhConsumed, Instant completedAt) {
        if (this.status == ChargeSessionStatus.COMPLETED) {
            throw new IllegalStateException("La sesion " + id + " ya esta cerrada");
        }
        Objects.requireNonNull(kwhConsumed, "kwhConsumed requerido");
        Objects.requireNonNull(completedAt, "completedAt requerido");
        this.kwhConsumed = kwhConsumed;
        this.completedAt = completedAt;
        this.status = ChargeSessionStatus.COMPLETED;
    }

    public SessionId id() { return id; }
    public UserId userId() { return userId; }
    public StationId stationId() { return stationId; }
    public Instant startedAt() { return startedAt; }
    public ChargeSessionStatus status() { return status; }
    public Kwh kwhConsumed() { return kwhConsumed; }
    public Instant completedAt() { return completedAt; }

    public boolean isCompleted() {
        return status == ChargeSessionStatus.COMPLETED;
    }
}
