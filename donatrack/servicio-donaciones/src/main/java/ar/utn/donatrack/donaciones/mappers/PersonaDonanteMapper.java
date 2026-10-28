package ar.utn.donatrack.donaciones.mappers;

import lombok.RequiredArgsConstructor;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaJuridicaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaJuridicaResponseDTO;
import ar.utn.donatrack.donaciones.models.donante.Documento;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.exceptions.comunes.TipoDesconocidoException;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.springframework.stereotype.Component;

import java.time.Period;

/**
 * Personas donantes (humanas y organizaciones) ↔ DTO. La dirección, los contactos
 * y los representantes los mapean sus propios mappers, que comparte con el de
 * entidades.
 */
@Component
@RequiredArgsConstructor
public class PersonaDonanteMapper {

    private final DireccionMapper direcciones;
    private final ContactoMapper contactos;
    private final RepresentanteMapper representantes;

    // ── REQUEST → MODELO ──────────────────────────────────────────────────────

    public PersonaDonante toModel(PersonaDonanteRequestDTO dto) {
        PersonaDonante persona = construir(dto);
        persona.definirContactoPredeterminado(contactos.toContacto(dto.getMedioContactoPredeterminado()));
        return persona;
    }

    private PersonaDonante construir(PersonaDonanteRequestDTO dto) {
        if (dto instanceof PersonaHumanaRequestDTO h) {
            return PersonaHumana.builder()
                    .nombre(h.getNombre())
                    .apellido(h.getApellido())
                    .genero(h.getGenero())
                    .fechaNacimiento(h.getFechaNacimiento())
                    .documento(Documento.de(h.getTipoDocumento(), h.getNumeroDocumento()))
                    .email(h.getEmail())
                    .direccion(direcciones.toDireccion(h.getDireccion()))
                    .build();
        }
        if (dto instanceof PersonaJuridicaRequestDTO j) {
            return PersonaJuridica.builder()
                    .razonSocial(j.getRazonSocial())
                    .email(j.getEmail())
                    .documento(Documento.deOrganizacion(j.getTipoDocumento(), j.getNumeroDocumento()))
                    .rubro(j.getRubro())
                    .tipo(j.getTipoOrganizacion())
                    .direccion(direcciones.toDireccion(j.getDireccion()))
                    .representantes(representantes.toRepresentantes(j.getRepresentantes()))
                    .build();
        }
        throw new TipoDesconocidoException("donante");
    }

    // ── MODELO → RESPONSE ─────────────────────────────────────────────────────

    public PersonaDonanteResponseDTO toDTO(PersonaDonante persona) {
        if (persona instanceof PersonaHumana h) {
            return PersonaHumanaResponseDTO.builder()
                    .id(h.getId())
                    .nombre(h.getNombre())
                    .apellido(h.getApellido())
                    .genero(h.getGenero())
                    .edad(h.getFechaNacimiento() != null
                            ? Period.between(h.getFechaNacimiento(), FechaHoraArgentina.hoy()).getYears()
                            : null)
                    .tipoDocumento(h.getTipoDocumento())
                    .numeroDocumento(h.getNumeroDocumento())
                    .email(h.getEmail())
                    .estado(h.getEstado().nombre())
                    .direccion(direcciones.toDireccionDTO(h.getDireccion()))
                    .medioContactoPredeterminado(contactos.toContactoDTO(h.getMedioContactoPredeterminado()))
                    .contactos(contactos.toContactosDTO(h.getContactos()))
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
                    .estado(j.getEstado().nombre())
                    .direccion(direcciones.toDireccionDTO(j.getDireccion()))
                    .representantes(representantes.toRepresentantesDTO(j.getRepresentantes()))
                    .medioContactoPredeterminado(contactos.toContactoDTO(j.getMedioContactoPredeterminado()))
                    .contactos(contactos.toContactosDTO(j.getContactos()))
                    .build();
        }
        throw new TipoDesconocidoException("donante");
    }
}
