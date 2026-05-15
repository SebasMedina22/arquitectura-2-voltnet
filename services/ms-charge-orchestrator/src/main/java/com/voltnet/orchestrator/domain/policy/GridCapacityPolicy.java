package com.voltnet.orchestrator.domain.policy;

import com.voltnet.orchestrator.domain.exception.ChargeRuleViolationException;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.StationLoadSnapshot;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.GridLoadPort;

/**
 * Implementa la Regla R1 del caso:
 *   "No se puede iniciar una carga si el estado actual de la estacion
 *    (verificado sincronamente) indica una carga total de red > 100 kW".
 *
 * Consulta MS-GridLoad via el puerto GridLoadPort. Rechaza si la
 * estacion esta sobrecargada (campo overloaded del snapshot, o
 * comparacion directa contra el umbral configurable como cinturon y tirantes).
 */
public class GridCapacityPolicy implements ChargeStartPolicy {

    public static final String CODE = "R1_GRID_OVERLOADED";

    private final GridLoadPort gridLoadPort;
    private final double thresholdKw;

    public GridCapacityPolicy(GridLoadPort gridLoadPort, double thresholdKw) {
        this.gridLoadPort = gridLoadPort;
        this.thresholdKw = thresholdKw;
    }

    @Override
    public void check(UserId userId, StationId stationId) {
        StationLoadSnapshot snapshot = gridLoadPort.fetchLoad(stationId);
        boolean overThreshold = snapshot.totalLoadKw().doubleValue() > thresholdKw;
        if (snapshot.overloaded() || overThreshold) {
            throw new ChargeRuleViolationException(
                    CODE,
                    "Estacion " + stationId + " sobrecargada (load=" + snapshot.totalLoadKw()
                            + " kW, umbral=" + thresholdKw + " kW). R1 bloquea el inicio."
            );
        }
    }

    @Override
    public String ruleCode() {
        return CODE;
    }
}
