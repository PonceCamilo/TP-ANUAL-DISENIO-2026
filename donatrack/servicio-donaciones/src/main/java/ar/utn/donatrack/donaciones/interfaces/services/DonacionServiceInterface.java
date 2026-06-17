package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;

import java.util.List;
import java.util.UUID;

public interface DonacionServiceInterface {
  List<DonacionResponseDTO> obtenerDonaciones(EstadoDonacion estado);
  DonacionResponseDTO obtenerPorId(UUID id);
  void cambiarEstado(UUID id, CambioEstadoRequestDTO dto);
  void modificarBien(UUID id, BienRequestDTO dto);
  void eliminar(UUID id);
}
