package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DireccionResponseDTO {

    private final String calle;
    private final int numero;
    private final String codigoPostal;
    private final LocalidadResponseDTO localidad;
}
