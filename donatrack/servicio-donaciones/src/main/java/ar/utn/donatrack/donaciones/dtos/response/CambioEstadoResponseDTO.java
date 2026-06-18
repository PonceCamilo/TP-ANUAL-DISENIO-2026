package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstadoResponseDTO {
  private EstadoDonacion estado;
  private String justificacion;
  private LocalDateTime fechaHora;
}