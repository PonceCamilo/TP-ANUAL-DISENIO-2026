package ar.utn.donatrack.donaciones.model.donacion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionConEstado extends Donacion{
  private Boolean esNuevo;
}
