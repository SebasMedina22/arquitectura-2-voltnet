package com.voltnet.orchestrator.domain.policy;

import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.UserId;

/**
 * Patron Strategy (GoF): contrato comun para las politicas que validan
 * el inicio de una sesion de carga. Hoy aplicamos dos (R1 y R2); manana
 * podriamos sumar (ej. ventanas horarias) sin tocar el caso de uso.
 */
public interface ChargeStartPolicy {

    /**
     * Valida la regla. Lanza ChargeRuleViolationException si no se cumple.
     */
    void check(UserId userId, StationId stationId);

    String ruleCode();
}
