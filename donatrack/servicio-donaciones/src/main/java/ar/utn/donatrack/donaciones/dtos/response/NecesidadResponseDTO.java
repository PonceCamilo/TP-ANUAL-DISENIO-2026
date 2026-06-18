package ar.utn.donatrack.donaciones.dtos.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NecesidadExtraordinariaResponseDTO.class, name = "EXTRAORDINARIA"),
        @JsonSubTypes.Type(value = NecesidadRecurrenteResponseDTO.class, name = "RECURRENTE")
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class NecesidadResponseDTO {

    private UUID id;
    private String nombre;
    private String descripcion;
    private Double cantidadObjetivo;
    private Double cantidadRecibida;
    private boolean satisfecha;
}