package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosPendientesResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import java.util.List;
import java.util.UUID;

public interface DonacionServiceInterface {
  PaginaResponseDTO<DonacionResponseDTO> obtenerDonaciones(FiltroDonaciones filtro, SolicitudPagina pagina);
  DonacionResponseDTO obtenerPorId(UUID id);
  void cambiarEstado(UUID id, CambioEstadoRequestDTO dto);
  void modificarBien(UUID id, BienRequestDTO dto);
  void eliminar(UUID id);
  CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion);
  List<CandidatosPendientesResponseDTO> obtenerCandidatosPendientes();
  void asignar(UUID idDonacion, AsignacionRequestDTO dto);
}
