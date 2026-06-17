package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EmailRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.LocalidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaJuridicaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.TelefonoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.WhatsappRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DireccionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EmailResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.LocalidadResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.MedioDeContactoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaJuridicaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.ProvinciaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.RepresentanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.TelefonoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.WhatsappResponseDTO;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import ar.utn.donatrack.donaciones.models.entidad.Provincia;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Component
public class PersonaDonanteMapper {

  // ── REQUEST → MODELO ──────────────────────────────────────────────────────

  public PersonaDonante toModel(PersonaDonanteRequestDTO dto) {
    if (dto instanceof PersonaHumanaRequestDTO h) {
      return PersonaHumana.builder()
          .nombre(h.getNombre())
          .apellido(h.getApellido())
          .genero(h.getGenero())
          .fechaNacimiento(h.getFechaNacimiento())
          .tipoDocumento(h.getTipoDocumento())
          .numeroDocumento(h.getNumeroDocumento())
          .email(h.getEmail())
          .direccion(toDireccion(h.getDireccion()))
          .medioContactoPredeterminado(toContacto(h.getMedioContactoPredeterminado()))
          .build();
    }
    if (dto instanceof PersonaJuridicaRequestDTO j) {
      return PersonaJuridica.builder()
          .razonSocial(j.getRazonSocial())
          .email(j.getEmail())
          .tipoDocumento(j.getTipoDocumento())
          .numeroDocumento(j.getNumeroDocumento())
          .rubro(j.getRubro())
          // CORRECCIÓN: era j.getTipo(), ahora j.getTipoOrganizacion()
          .tipo(j.getTipoOrganizacion())
          .direccion(toDireccion(j.getDireccion()))
          .representantes(toRepresentantes(j.getRepresentantes()))
          .medioContactoPredeterminado(toContacto(j.getMedioContactoPredeterminado()))
          .build();
    }
    throw new IllegalArgumentException("Tipo de donante desconocido");
  }

  // ── MODELO → RESPONSE ─────────────────────────────────────────────────────

  public PersonaDonanteResponseDTO toDTO(PersonaDonante persona) {
    if (persona instanceof PersonaHumana h) {
      return PersonaHumanaResponseDTO.builder()
          .id(h.getId())
          .nombre(h.getNombre())
          .apellido(h.getApellido())
          .genero(h.getGenero())
          .edad(Period.between(h.getFechaNacimiento(), LocalDate.now()).getYears())
          .tipoDocumento(h.getTipoDocumento())
          .numeroDocumento(h.getNumeroDocumento())
          .email(h.getEmail())
          .estado(h.getEstado())
          .direccion(toDireccionDTO(h.getDireccion()))
          .medioContactoPredeterminado(toContactoDTO(h.getMedioContactoPredeterminado()))
          .contactos(toContactosDTO(h.getContactos()))
          .build();
    }
    if (persona instanceof PersonaJuridica j) {
      return PersonaJuridicaResponseDTO.builder()
          .id(j.getId())
          .razonSocial(j.getRazonSocial())
          .email(j.getEmail())
          .tipoDocumento(j.getTipoDocumento())
          .numeroDocumento(j.getNumeroDocumento())
          .rubro(j.getRubro())
          .tipo(j.getTipo())
          .estado(j.getEstado())
          .direccion(toDireccionDTO(j.getDireccion()))
          .representantes(toRepresentantesDTO(j.getRepresentantes()))
          .medioContactoPredeterminado(toContactoDTO(j.getMedioContactoPredeterminado()))
          .contactos(toContactosDTO(j.getContactos()))
          .build();
    }
    throw new IllegalArgumentException("Tipo de donante desconocido");
  }

  // ── CONTACTOS ─────────────────────────────────────────────────────────────

  public MedioDeContacto toContacto(MedioDeContactoRequestDTO dto) {
    return switch (dto) {
      case EmailRequestDTO    ignored -> Email.builder().valor(dto.getValor()).build();
      case WhatsappRequestDTO ignored -> Whatsapp.builder().valor(dto.getValor()).build();
      case TelefonoRequestDTO ignored -> Telefono.builder().valor(dto.getValor()).build();
      case null, default -> throw new IllegalArgumentException("Tipo de contacto desconocido");
    };
  }

  private MedioDeContactoResponseDTO toContactoDTO(MedioDeContacto c) {
    if (c == null) return null;
    return switch (c) {
      case Email e    -> EmailResponseDTO.builder().valor(e.getValor()).build();
      case Telefono t -> TelefonoResponseDTO.builder().valor(t.getValor()).build();
      case Whatsapp w -> WhatsappResponseDTO.builder().valor(w.getValor()).build();
      default -> throw new IllegalArgumentException(
          "Tipo de contacto desconocido: " + c.getClass().getSimpleName());
    };
  }

  private List<MedioDeContactoResponseDTO> toContactosDTO(List<MedioDeContacto> contactos) {
    if (contactos == null) return new ArrayList<>();
    return contactos.stream().map(this::toContactoDTO).toList();
  }

  // ── DIRECCIÓN ─────────────────────────────────────────────────────────────

  private Direccion toDireccion(DireccionRequestDTO dto) {
    if (dto == null) return null;
    return Direccion.builder()
        .calle(dto.getCalle())
        .numero(dto.getNumero())
        .codigoPostal(dto.getCodigoPostal())
        .localidad(toLocalidad(dto.getLocalidad()))
        .build();
  }

  private Localidad toLocalidad(LocalidadRequestDTO dto) {
    if (dto == null) return null;
    return Localidad.builder()
        .nombre(dto.getNombre())
        .provincia(Provincia.builder()
            .nombre(dto.getProvincia().getNombre())
            .build())
        .build();
  }

  private DireccionResponseDTO toDireccionDTO(Direccion dir) {
    if (dir == null) return null;
    return DireccionResponseDTO.builder()
        .calle(dir.getCalle())
        .numero(dir.getNumero())
        .codigoPostal(dir.getCodigoPostal())
        .localidad(toLocalidadDTO(dir.getLocalidad()))
        .build();
  }

  private LocalidadResponseDTO toLocalidadDTO(Localidad localidad) {
    if (localidad == null) return null;
    return LocalidadResponseDTO.builder()
        .nombre(localidad.getNombre())
        .provincia(localidad.getProvincia() == null ? null :
            ProvinciaResponseDTO.builder()
                .nombre(localidad.getProvincia().getNombre())
                .build())
        .build();
  }

  // ── REPRESENTANTES ────────────────────────────────────────────────────────

  public Representante toRepresentante(RepresentanteRequestDTO dto) {
    if (dto == null) return null;
    return Representante.builder()
        .nombre(dto.getNombre())
        .apellido(dto.getApellido())
        .email(dto.getEmail())
        .build();
  }

  private List<Representante> toRepresentantes(List<RepresentanteRequestDTO> dtos) {
    if (dtos == null) return new ArrayList<>();
    return dtos.stream().map(this::toRepresentante).toList();
  }

  private List<RepresentanteResponseDTO> toRepresentantesDTO(List<Representante> representantes) {
    if (representantes == null) return new ArrayList<>();
    return representantes.stream()
        .map(r -> RepresentanteResponseDTO.builder()
            .nombre(r.getNombre())
            .apellido(r.getApellido())
            .email(r.getEmail())
            .contactos(toContactosDTO(r.getContactos()))
            .build())
        .toList();
  }
}