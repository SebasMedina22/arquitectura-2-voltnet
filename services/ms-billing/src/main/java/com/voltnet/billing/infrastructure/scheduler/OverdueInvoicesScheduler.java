package com.voltnet.billing.infrastructure.scheduler;

import com.voltnet.billing.application.port.in.MarkOverdueInvoicesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara periodicamente el caso de uso de marcar facturas vencidas.
 * No contiene logica de negocio - solo es el "reloj" que activa el use case.
 */
@Component
public class OverdueInvoicesScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueInvoicesScheduler.class);

    private final MarkOverdueInvoicesUseCase useCase;

    public OverdueInvoicesScheduler(MarkOverdueInvoicesUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${billing.scheduler.interval-ms:30000}",
               initialDelayString = "${billing.scheduler.initial-delay-ms:10000}")
    public void sweep() {
        int marked = useCase.markAllOverdue();
        if (marked > 0) {
            log.info("Marcadas {} facturas como OVERDUE", marked);
        }
    }
}
