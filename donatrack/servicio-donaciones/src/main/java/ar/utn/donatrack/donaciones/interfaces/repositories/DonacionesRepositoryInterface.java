package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;

import java.util.List;
import java.util.UUID;

public interface DonacionesRepositoryInterface {
  void cargarDonaciones(List<Donacion> donaciones);
  List<Donacion> obtenerTodas();
  List<Donacion> obtenerPorEstado(EstadoDonacion estado);
  List<Donacion> obtenerPorDonante(UUID idDonante);
  Donacion obtenerPorId(UUID id);
  void eliminar(UUID idDonacion);
}
