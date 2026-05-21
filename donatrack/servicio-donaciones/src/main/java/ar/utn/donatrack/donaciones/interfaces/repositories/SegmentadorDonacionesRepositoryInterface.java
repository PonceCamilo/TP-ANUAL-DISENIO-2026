package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import java.util.List;

public interface SegmentadorDonacionesRepositoryInterface {
  void cargarDonaciones(List<Donacion> donaciones);
}
