package ar.utn.donatrack.logistica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// este archivo es unicamente para la docu con swagger.

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Logistica")
                        .version("1.0.0")
                        .description("API para planificar rutas, gestionar la flota de camiones y el ciclo de vida de las entregas."))
                .servers(List.of(new Server()
                        .url("http://localhost:8085")
                        .description("Local")));
    }
}
