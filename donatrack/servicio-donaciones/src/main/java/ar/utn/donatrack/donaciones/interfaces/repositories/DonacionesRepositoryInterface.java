package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;

import java.util.List;
import java.util.UUID;

public interface DonacionesRepositoryInterface {
    void cargarDonaciones(List<Donacion> donaciones);
    List<Donacion> obtenerTodas();
    List<Donacion> obtenerPorEstado(EstadoDonacion estado);
    Donacion obtenerPorId(UUID id);
    void cambiarEstado(UUID idDonacion, EstadoDonacion nuevoEstado);
    void modificarBien(UUID idDonacion, Bien bien);
}

