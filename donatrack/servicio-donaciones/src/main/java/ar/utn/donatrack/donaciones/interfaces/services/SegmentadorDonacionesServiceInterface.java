package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import java.util.List;
import java.util.UUID;

public interface SegmentadorDonacionesServiceInterface {
  void segmentar(List<BienRequestDTO> bienes, UUID idDonante, String descripcion);
  void cargarDonaciones(List<Donacion> donaciones);
}
