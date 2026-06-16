package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.EmailDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaJuridicaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteDTO;
import ar.utn.donatrack.donaciones.dtos.request.TelefonoDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaJuridicaResponseDTO;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PersonaDonanteMapper {

  public PersonaDonante toModel(PersonaDonanteRequestDTO dto) {
    if (dto instanceof PersonaHumanaRequestDTO h) {
      return PersonaHumana.builder()
          .email(h.getEmail())
          .tipoDocumento(h.getTipoDocumento())
          .numeroDocumento(h.getNumeroDocumento())
          .nombre(h.getNombre())
          .apellido(h.getApellido())
          .estado(EstadoDonante.ACTIVO)
          .contactos(toContactos(h.getContactos()))
          .build();
    }
    if (dto instanceof PersonaJuridicaRequestDTO j) {
      return PersonaJuridica.builder()
          .email(j.getEmail())
          .tipoDocumento(j.getTipoDocumento())
          .numeroDocumento(j.getNumeroDocumento())
          .razonSocial(j.getRazonSocial())
          .estado(EstadoDonante.ACTIVO)
          .contactos(toContactos(j.getContactos()))
          .representantes(toRepresentantes(j.getRepresentantes()))
          .build();
    }
    throw new IllegalArgumentException("Tipo de donante desconocido");
  }

  public PersonaDonanteResponseDTO toDTO(PersonaDonante persona) {
    if (persona instanceof PersonaHumana h) {
      return PersonaHumanaResponseDTO.builder()
          .id(h.getId())
          .nombre(h.getNombre())
          .apellido(h.getApellido())
          .genero(h.getGenero())
          .edad(LocalDate.now().getYear() - h.getFechaNacimiento().getYear())
          .tipoDocumento(h.getTipoDocumento())
          .numeroDocumento(h.getNumeroDocumento())
          .direccion(toDireccionDTO(h.getDireccion()))
          .email(h.getEmail())
          .contactos(toContactosDTO(h.getContactos()))
          .estado(h.getEstado())

          .build();
    }
    if (persona instanceof PersonaJuridica j) {
      return PersonaJuridicaResponseDTO.builder()
          .id(j.getId())
          .razonSocial(j.getRazonSocial())
          .email(j.getEmail())
          .tipoDocumento(j.getTipoDocumento())
          .numeroDocumento(j.getNumeroDocumento())
          .direccion(toDireccionDTO(j.getDireccion()))
          .estado(j.getEstado())
          .contactos(toContactosDTO(j.getContactos()))
          .representantes(toRepresentantesDTO(j.getRepresentantes()))
          .rubro(j.getRubro())
          .tipo(j.getTipo())
          .build();
    }
    throw new IllegalArgumentException("Tipo de donante desconocido");
  }

  private List<MedioDeContacto> toContactos(List<MedioDeContactoDTO> dtos) {
    if (dtos == null) return new ArrayList<>();
    return dtos.stream().map(this::toContacto).toList();
  }

  public MedioDeContacto toContacto(MedioDeContactoDTO dto) {
    return switch (dto) {
      case null -> null;
      case EmailDTO emailDTO -> new Email();
      case TelefonoDTO telefonoDTO -> new Telefono(dto.getValor());
      default -> throw new IllegalArgumentException("DTO de contacto desconocido");
    };

    // ... repetir para Whatsapp si corresponde

  }

  private List<MedioDeContactoDTO> toContactosDTO(List<MedioDeContacto> contactos) {
    if (contactos == null) return new ArrayList<>();
    return contactos.stream()
        .map(c -> new MedioDeContactoDTO().setValor(c.getValor()))
        .toList();
  }

  private List<Representante> toRepresentantes(List<RepresentanteDTO> dtos) {
    if (dtos == null) return new ArrayList<>();
    return dtos.stream()
        .map(d -> new Representante(d.getNombre(), d.getApellido(), d.getEmail()))
        .toList();
  }

  public Representante toRepresentante(RepresentanteDTO dto) {
    return new Representante(dto.getNombre(), dto.getApellido(), dto.getEmail());
  }

  private List<RepresentanteDTO> toRepresentantesDTO(List<Representante> representantes) {
    if (representantes == null) return new ArrayList<>();
    return representantes.stream()
        .map(r -> new RepresentanteDTO(r.getNombre(), r.getApellido(), r.getEmail()))
        .toList();
  }
}