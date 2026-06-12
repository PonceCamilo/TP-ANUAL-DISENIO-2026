package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import java.util.List;

public interface DonacionesRepositoryInterface {
    void cargarDonaciones(List<Donacion> donaciones);
}
