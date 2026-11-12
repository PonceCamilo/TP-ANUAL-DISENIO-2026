package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.dtos.response.ErrorResponseDTO;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_ERROR = "ErrorResponseDTO";
    private static final String ESQUEMA_TOKEN = "token";

    /** Rutas que no piden token (SeguridadConfig): no llevan candado ni los 401 y 403. */
    private static final List<String> RUTAS_PUBLICAS = List.of("/logistica/eventos");

    /**
     * Entrega 6: el botón Authorize de Swagger acepta el JWT del Servicio de
     * Autenticación (sin el prefijo Bearer) y lo manda en cada pedido.
     */
    @Bean
    public OpenAPI donacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Donaciones")
                        .description("Gestión de personas donantes, donaciones, entidades beneficiarias y sus necesidades. "
                                + "Cada pedido lleva el JWT del Servicio de Autenticación en Authorization: Bearer; "
                                + "qué puede hacer cada rol está en la descripción de cada grupo.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT emitido por el Servicio de Autenticación")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_TOKEN));
    }

    /**
     * Toda respuesta de error tiene la misma forma (ErrorResponseDTO, la arma
     * GlobalExceptionHandler). En lugar de repetirla en cada endpoint se agrega acá
     * una sola vez: cada respuesta 4xx/5xx que declara un controller recibe ese
     * esquema (springdoc le ponía el tipo de éxito del método, que en un error no
     * llega nunca), y toda operación documenta además el 500. Los controllers solo
     * dicen qué errores puede dar cada operación y por qué.
     */
    @Bean
    public OpenApiCustomizer respuestasDeError() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance()
                    .readAll(new AnnotatedType(ErrorResponseDTO.class))
                    .forEach(openApi.getComponents()::addSchemas);
            Content error = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR)));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((camino, ruta) -> ruta.readOperations().forEach(operacion -> {
                boolean publica = RUTAS_PUBLICAS.stream().anyMatch(camino::startsWith);
                if (publica) {
                    operacion.setSecurity(List.of());
                } else {
                    operacion.getResponses().computeIfAbsent("401",
                            codigo -> new ApiResponse().description("Falta el token o no es válido"));
                    operacion.getResponses().computeIfAbsent("403",
                            codigo -> new ApiResponse().description("El rol o el perfil del token no permite la operación"));
                }
                operacion.getResponses().computeIfAbsent("500",
                        codigo -> new ApiResponse().description("Error inesperado del servicio"));
                operacion.getResponses().forEach((codigo, respuesta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) {
                        respuesta.setContent(error);
                    }
                });
            }));
        };
    }
}
