package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;

import java.util.List;
import java.util.UUID;

public interface SegmentadorDonacionesServiceInterface {
  void segmentar(List<Bien> bienesASegmentar, UUID idDonante);
  void cargarDonaciones(List<Donacion> donaciones);
}
