package ar.utn.donatrack.logisticaexterna.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// este archivo es unicamente para la docu con swagger.
// Sin servers fijos: Swagger usa la URL desde la que se abre (local o Railway).

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logisticaExternaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Proveedor de Logística Externo")
                        .version("1.0.0")
                        .description("API de un proveedor de logística de terceros: alta de envíos, tracking y ciclo de vida de la entrega."));
    }
}
