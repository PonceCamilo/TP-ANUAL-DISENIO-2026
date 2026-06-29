package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import java.util.List;
import java.util.UUID;

public interface DonacionesRepositoryInterface {
  void cargarDonaciones(List<Donacion> donaciones);
  List<Donacion> obtenerTodas();
  List<Donacion> obtenerPorEstado(String estado);
  List<Donacion> obtenerPorDonante(UUID idDonante);
  Donacion obtenerPorId(UUID id);
  void eliminar(UUID idDonacion);
}
