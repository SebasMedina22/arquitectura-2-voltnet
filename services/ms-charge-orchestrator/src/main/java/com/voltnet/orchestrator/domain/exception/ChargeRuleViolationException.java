package com.voltnet.orchestrator.domain.exception;

/**
 * Se lanza cuando una de las reglas de negocio del caso (R1 o R2) bloquea
 * el inicio de una sesion de carga. Mapeado a HTTP 422 por el controlador.
 */
public class ChargeRuleViolationException extends RuntimeException {

    private final String ruleCode;

    public ChargeRuleViolationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String ruleCode() {
        return ruleCode;
    }
}
