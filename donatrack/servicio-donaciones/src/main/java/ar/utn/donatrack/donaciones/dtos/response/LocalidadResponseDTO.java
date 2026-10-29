package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocalidadResponseDTO {

    private final String nombre;
    private final ProvinciaResponseDTO provincia;
}
