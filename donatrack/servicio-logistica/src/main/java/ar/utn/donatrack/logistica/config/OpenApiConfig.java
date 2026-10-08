package ar.utn.donatrack.logistica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// este archivo es unicamente para la docu con swagger.
// Sin servers fijos: Swagger usa la URL desde la que se abre (local o Railway).

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Logistica")
                        .version("1.0.0")
                        .description("API para planificar rutas, gestionar la flota de camiones y el ciclo de vida de las entregas."));
    }
}
