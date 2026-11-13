package ar.utn.donatrack.logistica.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// este archivo es unicamente para la docu con swagger.
// Sin servers fijos: Swagger usa la URL desde la que se abre (local o Railway).
// Entrega 6: los endpoints piden el JWT de Autenticación (botón "Authorize").
// El callback de planificación usa su token de correlación y /ruteo no pide nada.

@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_TOKEN = "token";

    @Bean
    public OpenAPI logisticaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Logistica")
                        .version("1.0.0")
                        .description("API para planificar rutas, gestionar la flota de camiones y el ciclo de vida de las entregas. "
                                + "Cada pedido lleva el JWT del Servicio de Autenticación (Authorization: Bearer). "
                                + "Una entidad beneficiaria solo ve y confirma sus entregas. El broker planifica con la "
                                + "clave entre servicios (X-DonaTrack-Servicio)."))
                .components(new Components().addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
    }
}
