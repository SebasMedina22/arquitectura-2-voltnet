package com.voltnet.orchestrator.domain.policy;

import com.voltnet.orchestrator.domain.exception.ChargeRuleViolationException;
import com.voltnet.orchestrator.domain.model.StationId;
import com.voltnet.orchestrator.domain.model.User;
import com.voltnet.orchestrator.domain.model.UserId;
import com.voltnet.orchestrator.domain.port.out.UserRepository;

/**
 * Implementa la Regla R2 del caso:
 *   "El sistema debe verificar que el usuario tenga un metodo de pago activo;
 *    si tiene deudas vencidas de mas de 30 dias, el inicio de carga se bloquea
 *    automaticamente".
 *
 * Consulta la proyeccion local (tabla users de MySQL) — no llama
 * sincronamente a Billing. La proyeccion la mantiene un consumer AMQP de
 * UserDebtUpdated. Si el usuario no existe en la proyeccion, se rechaza
 * por defecto (politica conservadora alineada con "en duda, rechazar").
 */
public class UserSolvencyPolicy implements ChargeStartPolicy {

    public static final String CODE = "R2_USER_NOT_SOLVENT";

    private final UserRepository userRepository;
    private final int overdueBlockingDays;

    public UserSolvencyPolicy(UserRepository userRepository, int overdueBlockingDays) {
        this.userRepository = userRepository;
        this.overdueBlockingDays = overdueBlockingDays;
    }

    @Override
    public void check(UserId userId, StationId stationId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ChargeRuleViolationException(
                        CODE,
                        "Usuario " + userId + " sin proyeccion local de solvencia. R2 rechaza por defecto."
                ));

        if (!user.hasActivePaymentMethod()) {
            throw new ChargeRuleViolationException(
                    CODE,
                    "Usuario " + userId + " no tiene metodo de pago activo. R2 bloquea."
            );
        }
        if (user.overdueDays() > overdueBlockingDays) {
            throw new ChargeRuleViolationException(
                    CODE,
                    "Usuario " + userId + " tiene deuda vencida " + user.overdueDays()
                            + " dias (> " + overdueBlockingDays + "). R2 bloquea."
            );
        }
    }

    @Override
    public String ruleCode() {
        return CODE;
    }
}
