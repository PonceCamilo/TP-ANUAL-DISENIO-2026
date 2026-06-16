package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class MedioDeContactoResponseDTO {

  protected String valor;
}
