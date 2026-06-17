package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.validations.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonacionService implements DonacionServiceInterface {

  private final DonacionesRepositoryInterface repositorio;
  private final DonacionesValidator validador;
  private final DonacionMapper mapper;

  public List<DonacionResponseDTO> obtenerDonaciones(EstadoDonacion estado) {
    if (estado != null) {
      return mapper.toDTOList(repositorio.obtenerPorEstado(estado));
    }
    return mapper.toDTOList(repositorio.obtenerTodas());
  }

  public DonacionResponseDTO obtenerPorId(UUID id) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    return mapper.toDTO(donacion);
  }

  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }

    validador.validarTransicion(donacion.getEstado(), dto.getEstado(), dto.getJustificacion());

    donacion.getHistorialEstados().add(CambioEstado.builder()
        .estado(dto.getEstado())
        .justificacion(dto.getJustificacion())
        .build());
    donacion.setEstado(dto.getEstado());
  }

  public void modificarBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    donacion.getBienes().set(0, mapper.toBien(dto));
  }

  public void eliminar(UUID id) {
    if (repositorio.obtenerPorId(id) == null) {
      throw new DonacionNoEncontradaException(id);
    }
    repositorio.eliminar(id);
  }
}
