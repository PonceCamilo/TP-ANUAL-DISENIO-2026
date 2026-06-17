package ar.utn.donatrack.donaciones.models.contacto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class MedioDeContacto {

  protected String valor;
}
