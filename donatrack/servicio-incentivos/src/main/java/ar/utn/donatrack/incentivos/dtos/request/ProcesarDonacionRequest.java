package ar.utn.donatrack.incentivos.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// se agregan los @Schema que sirven para docu de swagger. Da una descripcion y ejemplos en json de que es donanteId, destinatario, medio, etc
// se agrega notnull para que el campo no pueda venir vacio, lo mismo notblank
// se podrain agregar mas validaciones para que por ej cantidad de bienes no sea negativa (@PositiveOrZero)

@Schema(description = "Evento enviado por servicio-donaciones para actualizar incentivos")
public record ProcesarDonacionRequest(
        @Schema(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
        @NotNull UUID donanteId,

        @Schema(description = "Contacto del donante para notificaciones", example = "donante@mail.com")
        @NotBlank String destinatario,

        @Schema(description = "Medio de notificacion", example = "EMAIL")
        @NotBlank String medio,

        @Schema(description = "Cantidad de bienes registrados en la donacion", example = "3")
        int cantidadBienes,

        @Schema(description = "Categorias de los bienes donados", example = "[\"ALIMENTOS\", \"ABRIGO\"]")
        List<String> categoriasDonadas
) {}
