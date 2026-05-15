package com.voltnet.billing.application.config;

import com.voltnet.billing.application.port.in.MarkOverdueInvoicesUseCase;
import com.voltnet.billing.application.port.in.ProcessChargeSessionUseCase;
import com.voltnet.billing.application.port.in.QueryInvoicesUseCase;
import com.voltnet.billing.application.service.MarkOverdueInvoicesService;
import com.voltnet.billing.application.service.ProcessChargeSessionService;
import com.voltnet.billing.application.service.QueryInvoicesService;
import com.voltnet.billing.domain.port.out.InvoiceRepository;
import com.voltnet.billing.domain.port.out.UserDebtEventPublisher;
import com.voltnet.billing.domain.port.out.UserDebtRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;

/**
 * Wiring de casos de uso. Aisla la capa application de anotaciones Spring
 * (los services son Java puro, testeables sin contexto).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProcessChargeSessionUseCase processChargeSessionUseCase(
            InvoiceRepository invoiceRepository,
            Clock clock,
            @Value("${billing.tariff.rate:500.00}") BigDecimal ratePerKwh,
            @Value("${billing.tariff.currency:COP}") String currency,
            @Value("${billing.invoice.due-after:PT2M}") Duration dueAfter) {
        return new ProcessChargeSessionService(invoiceRepository, clock, ratePerKwh, currency, dueAfter);
    }

    @Bean
    public MarkOverdueInvoicesUseCase markOverdueInvoicesUseCase(
            InvoiceRepository invoiceRepository,
            UserDebtRepository userDebtRepository,
            UserDebtEventPublisher publisher,
            Clock clock) {
        return new MarkOverdueInvoicesService(invoiceRepository, userDebtRepository, publisher, clock);
    }

    @Bean
    public QueryInvoicesUseCase queryInvoicesUseCase(InvoiceRepository invoiceRepository) {
        return new QueryInvoicesService(invoiceRepository);
    }
}
