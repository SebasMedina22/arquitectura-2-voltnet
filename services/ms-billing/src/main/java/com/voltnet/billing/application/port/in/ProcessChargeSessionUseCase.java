package com.voltnet.billing.application.port.in;

import com.voltnet.billing.domain.event.ChargeSessionCompletedEvent;
import com.voltnet.billing.domain.model.Invoice;

public interface ProcessChargeSessionUseCase {

    /**
     * Procesa un evento de cierre de sesion creando una factura.
     * Idempotente: si ya existe factura para sessionId, devuelve la existente
     * sin reemplazarla (sustento de R3).
     */
    Invoice process(ChargeSessionCompletedEvent event);
}
