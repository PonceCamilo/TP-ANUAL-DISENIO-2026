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
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_ERROR = "ErrorResponseDTO";

    @Bean
    public OpenAPI donacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DonaTrack - Servicio de Donaciones")
                        .description("Gestión de personas donantes, donaciones, entidades beneficiarias y sus necesidades.")
                        .version("v1"));
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
            openApi.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(operacion -> {
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
