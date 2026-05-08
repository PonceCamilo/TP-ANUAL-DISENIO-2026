package ar.utn.donatrack.donaciones.repository;

import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;

import java.util.List;

public class SegmentadorDonacionesRepository {

  private static List<Donacion> donaciones;

  public SegmentadorDonacionesRepository(List<Donacion> donaciones) {
    this.donaciones = donaciones;
  }

  public static void cargarDonaciones(List<Donacion> listaDonaciones) {

    donaciones.add((Donacion) listaDonaciones);
  }
}
