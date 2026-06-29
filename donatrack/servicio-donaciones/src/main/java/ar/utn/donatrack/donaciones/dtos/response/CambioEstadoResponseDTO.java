package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstadoResponseDTO {
  private String estadoPrevio;
  private String estado;
  private String nombreTransicion;
  private String justificacion;
  private LocalDateTime fechaHora;
}