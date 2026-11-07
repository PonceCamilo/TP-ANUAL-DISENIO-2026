package ar.utn.donatrack.brokerlogistica.config;

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
    public OpenAPI brokerLogisticaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Broker de Logística")
                        .version("1.0.0")
                        .description("Intermediario entre Donaciones y los servicios de logística: elige el proveedor, "
                                + "traduce los pedidos de envío y normaliza los eventos de entrega."))
                .servers(List.of(new Server()
                        .url("http://localhost:8087")
                        .description("Local")));
    }
}
