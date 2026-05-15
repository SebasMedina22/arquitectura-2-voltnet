package com.voltnet.orchestrator.domain.policy;

import com.voltnet.orchestrator.domain.exception.ChargeRuleViolationException;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.StationLoadSnapshot;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.GridLoadPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GridCapacityPolicyTest {

    @Test
    void permite_cuandoCargaBajaUmbral() {
        GridLoadPort port = id -> new StationLoadSnapshot(id, new BigDecimal("75.0"), false);
        GridCapacityPolicy policy = new GridCapacityPolicy(port, 100.0);
        assertDoesNotThrow(() -> policy.check(new UserId("USR-001"), new StationId("STN-001")));
    }

    @Test
    void rechaza_cuandoCargaSuperaUmbral() {
        GridLoadPort port = id -> new StationLoadSnapshot(id, new BigDecimal("120.0"), true);
        GridCapacityPolicy policy = new GridCapacityPolicy(port, 100.0);
        ChargeRuleViolationException ex = assertThrows(ChargeRuleViolationException.class,
                () -> policy.check(new UserId("USR-001"), new StationId("STN-002")));
        assertEquals("R1_GRID_OVERLOADED", ex.ruleCode());
    }

    @Test
    void rechaza_cuandoOverloadedAunqueValorNumericoBajo() {
        GridLoadPort port = id -> new StationLoadSnapshot(id, new BigDecimal("50.0"), true);
        GridCapacityPolicy policy = new GridCapacityPolicy(port, 100.0);
        assertThrows(ChargeRuleViolationException.class,
                () -> policy.check(new UserId("USR-001"), new StationId("STN-001")));
    }
}
