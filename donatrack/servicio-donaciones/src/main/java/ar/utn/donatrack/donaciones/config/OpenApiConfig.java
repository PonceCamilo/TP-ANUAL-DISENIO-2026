package ar.utn.donatrack.donaciones.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI donacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Donaciones")
                        .description("Gestión de personas donantes, donaciones, entidades beneficiarias y sus necesidades.")
                        .version("v1"));
    }
}
