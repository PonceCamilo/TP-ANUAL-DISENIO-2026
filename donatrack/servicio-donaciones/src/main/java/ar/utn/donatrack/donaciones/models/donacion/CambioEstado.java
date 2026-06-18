package ar.utn.donatrack.donaciones.models.donacion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

public class CambioEstado {
  private final EstadoDonacion estado;
  private final String justificacion;

  @Builder.Default
  private final LocalDateTime fechaHora = LocalDateTime.now();
}
