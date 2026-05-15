package com.voltnet.orchestrator.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orchestratorOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("VoltNet - MS-ChargeOrchestrator")
                .description("Microservicio principal hexagonal. Orquesta R1 (capacidad de red via Feign+CB), "
                        + "R2 (proyeccion local de solvencia alimentada por eventos) y R3 (cierre resiliente "
                        + "via Transactional Outbox).")
                .version("0.1.0"));
    }
}
