package com.voltnet.billing.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("VoltNet - MS-Billing API")
                .version("0.1.0")
                .description("Microservicio asincrono de facturacion. " +
                        "Consume ChargeSessionCompleted via AMQP y publica UserDebtUpdated. " +
                        "Estos endpoints son de consulta interna; no son consumidos por otros MS."));
    }
}
