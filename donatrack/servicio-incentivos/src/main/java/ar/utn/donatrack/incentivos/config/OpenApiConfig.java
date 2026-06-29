package ar.utn.donatrack.incentivos.config;

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
    public OpenAPI incentivosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Incentivos")
                        .version("1.0.0")
                        .description("API para consultar metricas, misiones, insignias y procesar eventos de donaciones."))
                .servers(List.of(new Server()
                        .url("http://localhost:8083")
                        .description("Local")));
    }
}
