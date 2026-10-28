package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class NecesidadExtraordinariaResponseDTO extends NecesidadResponseDTO {
    // Hereda todo de NecesidadResponseDTO.
    // Al instanciarse, Spring le agregará automáticamente "tipo": "EXTRAORDINARIA" al JSON.
}