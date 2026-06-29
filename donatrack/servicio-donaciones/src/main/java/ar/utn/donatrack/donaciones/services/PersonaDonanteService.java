package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonaDonanteService implements PersonaDonanteServiceInterface {

  private final PersonaDonanteRepositoryInterface repositorio;
  private final PersonasValidator validador;
  private final PersonaDonanteMapper mapper;

  @Override
  public UUID registrar(PersonaDonanteRequestDTO dto) {
    validador.validarEmail(dto.getEmail());

    PersonaDonante persona = mapper.toModel(dto);
    persona.setId(UUID.randomUUID());
    persona.setEstado(EstadoDonante.ACTIVO);
    persona.setUltimaInteraccion(LocalDateTime.now());

    repositorio.guardar(persona);
    return persona.getId();
  }

  @Override
  public PersonaDonanteResponseDTO obtenerDonante(UUID id) {
    return mapper.toDTO(buscarOFallar(id));
  }

  @Override
  public List<PersonaDonanteResponseDTO> obtenerDonantes(EstadoDonante estado) {
    List<PersonaDonante> donantes = (estado != null)
        ? repositorio.obtenerPorEstado(estado)
        : repositorio.obtenerTodosDonantes();
    return donantes.stream().map(mapper::toDTO).toList();
  }

  @Override
  public void cambiarEstado(UUID id, EstadoDonanteRequestDTO dto) {
    PersonaDonante persona = buscarOFallar(id);
    validador.validarCambioEstado(persona.getEstado(), dto.getEstado(), dto.getJustificacion());
    repositorio.cambiarEstado(id, dto.getEstado());
    persona.setUltimaInteraccion(LocalDateTime.now());
  }

  @Override
  public void modificarContacto(UUID id, MedioDeContactoRequestDTO dto) {
    PersonaDonante persona = buscarOFallar(id);
    MedioDeContacto medio = mapper.toContacto(dto);
    validador.validarMedioContacto(medio);
    repositorio.modificarMedioContacto(id, medio);
    persona.setUltimaInteraccion(LocalDateTime.now());
  }

  @Override
  public void modificarRepresentante(UUID id, RepresentanteRequestDTO dto) {
    validador.validarExistenciaPersona(id);
    validador.validarEsPersonaJuridica(id);
    repositorio.modificarRepresentante(id, mapper.toRepresentante(dto));
  }

  @Override
  public PersonaDonanteResponseDTO actualizar(UUID id, PersonaDonanteUpdateRequestDTO dto) {
    PersonaDonante persona = buscarOFallar(id);

    if (persona instanceof PersonaHumana humana) {
      if (dto.getNombre() != null) humana.setNombre(dto.getNombre());
      if (dto.getApellido() != null) humana.setApellido(dto.getApellido());
      if (dto.getFechaNacimiento() != null) humana.setFechaNacimiento(dto.getFechaNacimiento());
    } else if (persona instanceof PersonaJuridica juridica) {
      if (dto.getRazonSocial() != null) juridica.setRazonSocial(dto.getRazonSocial());
      if (dto.getRubro() != null) juridica.setRubro(dto.getRubro());
    }

    if (dto.getDireccion() != null) {
      persona.setDireccion(mapper.toDireccion(dto.getDireccion()));
    }
    if (dto.getContactos() != null) {
      persona.setContactos(dto.getContactos().stream()
          .map(mapper::toContacto)
          .collect(Collectors.toCollection(ArrayList::new)));
    }
    persona.setUltimaInteraccion(LocalDateTime.now());

    repositorio.guardar(persona);
    return mapper.toDTO(persona);
  }

  @Override
  public void eliminar(UUID id) {
    validador.validarExistenciaPersona(id);
    repositorio.eliminar(id);
  }

  private PersonaDonante buscarOFallar(UUID id) {
    PersonaDonante persona = repositorio.obtenerPersona(id);
    if (persona == null) {
      throw new PersonaDonanteNoEncontradaException(id);
    }
    return persona;
  }
}