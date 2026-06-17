package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NecesidadExtraordinariaRequestDTO.class, name = "EXTRAORDINARIA"),
        @JsonSubTypes.Type(value = NecesidadRecurrenteRequestDTO.class, name = "RECURRENTE")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class NecesidadRequestDTO {
    @NotBlank(message = "El nombre de la necesidad es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La cantidad objetivo es obligatoria")
    @Min(value = 1, message = "La cantidad objetivo debe ser mayor a 0")
    private Integer cantidadObjetivo;
}