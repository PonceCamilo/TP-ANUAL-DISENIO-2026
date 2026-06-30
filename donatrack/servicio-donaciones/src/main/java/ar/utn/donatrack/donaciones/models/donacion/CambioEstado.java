package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.estado.EstadoDonacionBase;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstado {
  private final EstadoDonacionBase estadoPrevio;
  private final EstadoDonacionBase estado;
  private final String nombreTransicion;
  private final String justificacion;

  @Builder.Default
  private final LocalDateTime fechaHora = FechaHoraArgentina.ahora();
}
