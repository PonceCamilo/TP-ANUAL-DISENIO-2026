package ar.utn.donatrack.donaciones.model.donante;

import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;

import java.util.List;

public abstract class PersonaDonante {
  protected int idPersonaDonante;
  protected List<Donacion> donaciones;
  protected CargaDonacion cargaDonacion;
}
