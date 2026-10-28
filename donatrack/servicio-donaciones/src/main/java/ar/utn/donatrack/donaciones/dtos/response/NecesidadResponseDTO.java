package ar.utn.donatrack.donaciones.dtos.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NecesidadExtraordinariaResponseDTO.class, name = "EXTRAORDINARIA"),
        @JsonSubTypes.Type(value = NecesidadRecurrenteResponseDTO.class, name = "RECURRENTE")
})
@Getter
@SuperBuilder
public abstract class NecesidadResponseDTO {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final String subcategoria;
    private final int cantidadObjetivo;
    private final int cantidadRecibida;
    private final boolean satisfecha;
}