package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;

import java.util.List;

public interface SegmentadorDonacionesServiceInterface {
  void segmentar(CargaDonacion carga);
  void cargarDonaciones(List<Donacion> donaciones);
}
