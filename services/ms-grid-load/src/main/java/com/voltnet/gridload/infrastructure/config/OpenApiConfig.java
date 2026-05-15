package com.voltnet.gridload.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gridLoadOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("VoltNet - MS-GridLoad API")
                .version("0.1.0")
                .description("Consulta y simulacion de carga por estacion electrica. " +
                        "Fuente de verdad para la regla R1 (umbral 100 kW)."));
    }
}
