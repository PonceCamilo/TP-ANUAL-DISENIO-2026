package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.mappers.RepresentanteMapper;
import ar.utn.donatrack.donaciones.mappers.DireccionMapper;
import ar.utn.donatrack.donaciones.mappers.ContactoMapper;
import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConDonacionesException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class PersonaDonanteService {

  private final RepositorioPersonasDonantes repositorio;
  private final RepositorioDonaciones donaciones;
  private final PersonasValidator validador;
  private final PersonaDonanteMapper mapper;
  private final DireccionMapper direcciones;
  private final ContactoMapper contactos;
  private final RepresentanteMapper representantes;

  public PersonaDonanteResponseDTO registrar(PersonaDonanteRequestDTO dto) {
    validador.validarEmail(dto.getEmail());

    PersonaDonante persona = mapper.toModel(dto);
    persona.registrarInteraccion();

    repositorio.guardar(persona);
    return mapper.toDTO(persona);
  }

  @Transactional(readOnly = true)
  public PersonaDonanteResponseDTO obtenerDonante(UUID id) {
    return mapper.toDTO(validador.validarYObtenerPersona(id));
  }

  @Transactional(readOnly = true)
  public PaginaResponseDTO<PersonaDonanteResponseDTO> obtenerDonantes(String estado, SolicitudPagina pagina) {
    return PaginaResponseDTO.desde(repositorio.buscar(estado, pagina).map(mapper::toDTO));
  }

  public void cambiarEstado(UUID id, EstadoDonanteRequestDTO dto) {
    PersonaDonante persona = validador.validarYObtenerPersona(id);
    persona.cambiarEstado(dto.getEstado(), dto.getJustificacion());
    persona.registrarInteraccion();
    repositorio.guardar(persona);
  }

  public void modificarContacto(UUID id, MedioDeContactoRequestDTO dto) {
    PersonaDonante persona = validador.validarYObtenerPersona(id);
    MedioDeContacto medio = contactos.toContacto(dto);
    validador.validarMedioContacto(medio);

    // Todo sobre la MISMA instancia y un solo guardado: delegar el reemplazo en
    // el repositorio haría que trabajara sobre otra instancia, y este guardado
    // pisaría el cambio de contacto.
    persona.reemplazarContacto(medio);
    persona.registrarInteraccion();
    repositorio.guardar(persona);
  }

  public void modificarRepresentante(UUID id, RepresentanteRequestDTO dto) {
    // Una sola lectura: el validator devuelve la persona jurídica (404 si no
    // existe, 400 si es humana) y el representante se agrega en el modelo.
    PersonaJuridica juridica = validador.validarYObtenerJuridica(id);
    juridica.agregarRepresentante(representantes.toRepresentante(dto));
    juridica.registrarInteraccion();
    repositorio.guardar(juridica);
  }

  public PersonaDonanteResponseDTO actualizar(UUID id, PersonaDonanteUpdateRequestDTO dto) {
    PersonaDonante persona = validador.validarYObtenerPersona(id);

    if (persona instanceof PersonaHumana humana) {
      humana.actualizarDatosPersonales(dto.getNombre(), dto.getApellido(), dto.getFechaNacimiento(),
          dto.getGenero());
    } else if (persona instanceof PersonaJuridica juridica) {
      juridica.actualizarDatosDeOrganizacion(dto.getRazonSocial(), dto.getRubro(), dto.getTipoOrganizacion());
    }

    if (dto.getDireccion() != null) {
      persona.cambiarDireccion(direcciones.toDireccion(dto.getDireccion()));
    }
    if (dto.getContactos() != null) {
      persona.reemplazarContactos(contactos.toContactos(dto.getContactos()));
    }
    persona.registrarInteraccion();

    repositorio.guardar(persona);
    return mapper.toDTO(persona);
  }

  public void eliminar(UUID id) {
    PersonaDonante persona = validador.validarYObtenerPersona(id);
    // Sin FK entre donacion y persona_donante: la integridad la cuida el servicio.
    if (donaciones.existenDelDonante(id)) {
      throw new PersonaConDonacionesException(id);
    }
    repositorio.eliminar(persona);
  }
}