package ar.utn.donatrack.donaciones.models.donacion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionConEstado extends Donacion{
  private boolean esNuevo;
}
