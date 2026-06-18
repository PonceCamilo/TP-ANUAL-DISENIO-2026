package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponseDTO {
  private final LocalDateTime timestamp;
  private final int status;
  private final String error;
  private final String message;
}
