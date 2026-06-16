package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import lombok.Getter;

@Getter
public class EstadoDonanteResponseDTO {

  private EstadoDonante estado;
}
