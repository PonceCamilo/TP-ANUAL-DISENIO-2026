package ar.utn.donatrack.donaciones.dtos.request;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class NecesidadExtraordinariaRequestDTO extends NecesidadRequestDTO {
    // Para crearla no se necesita nada extra, hereda todo de la clase padre.
}