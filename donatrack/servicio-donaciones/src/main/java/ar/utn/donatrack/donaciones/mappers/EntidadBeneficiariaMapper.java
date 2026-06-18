package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.*;
import ar.utn.donatrack.donaciones.dtos.response.*;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntidadBeneficiariaMapper {

    private final PersonaDonanteMapper dependenciasMapper;

    // ── ENTIDAD ─────────────────────────────────────────────────────────────

    public EntidadBeneficiaria toModel(EntidadBeneficiariaRequestDTO dto) {
        return EntidadBeneficiaria.builder()
                .razonSocial(dto.getRazonSocial())
                .direccion(dependenciasMapper.toDireccion(dto.getDireccion()))
                .contactos(dto.getContactos() != null ?
                        dto.getContactos().stream().map(dependenciasMapper::toContacto).toList() : new ArrayList<>())
                .representantes(dependenciasMapper.toRepresentantes(dto.getRepresentantes()))
                .campanias(new ArrayList<>())
                .build();
    }

    public EntidadBeneficiariaResponseDTO toDTO(EntidadBeneficiaria entidad) {
        return EntidadBeneficiariaResponseDTO.builder()
                .id(entidad.getId())
                .razonSocial(entidad.getRazonSocial())
                .direccion(dependenciasMapper.toDireccionDTO(entidad.getDireccion()))
                .contactos(dependenciasMapper.toContactosDTO(entidad.getContactos()))
                .representantes(dependenciasMapper.toRepresentantesDTO(entidad.getRepresentantes()))
                .campanias(entidad.getCampanias() != null ?
                        entidad.getCampanias().stream().map(this::toCampaniaDTO).toList() : new ArrayList<>())
                .build();
    }

    // ── CAMPAÑAS ────────────────────────────────────────────────────────────

    public Campania toCampaniaModel(CampaniaRequestDTO dto, UUID idEntidad) {
        Campania campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(idEntidad);
        campania.setDescripcionGeneral(dto.getDescripcionGeneral());
        campania.setFechaInicio(dto.getFechaInicio());
        campania.setFechaFin(dto.getFechaFin());
        campania.setNecesidades(new ArrayList<>());
        return campania;
    }

    private CampaniaResponseDTO toCampaniaDTO(Campania campania) {
        return CampaniaResponseDTO.builder()
                .idCampania(campania.getIdCampania())
                .idEntidad(campania.getIdEntidad())
                .fechaInicio(campania.getFechaInicio())
                .fechaFin(campania.getFechaFin())
                .descripcionGeneral(campania.getDescripcionGeneral())
                .necesidades(campania.getNecesidades() != null ?
                        campania.getNecesidades().stream().map(this::toNecesidadDTO).toList() : new ArrayList<>())
                .build();
    }

    // ── NECESIDADES ─────────────────────────────────────────────────────────

    public Necesidad toNecesidadModel(NecesidadRequestDTO dto) {

        if (dto instanceof NecesidadExtraordinariaRequestDTO extraDto) {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre(extraDto.getNombre());
            necesidad.setDescripcion(extraDto.getDescripcion());
            necesidad.setCantidadObjetivo(extraDto.getCantidadObjetivo());
            // Toda necesidad nueva arranca con 0 recibido
            necesidad.setCantidadRecibida(0);
            return necesidad;

        } else if (dto instanceof NecesidadRecurrenteRequestDTO recuDto) {
            NecesidadRecurrente necesidad = new NecesidadRecurrente();
            necesidad.setNombre(recuDto.getNombre());
            necesidad.setDescripcion(recuDto.getDescripcion());
            necesidad.setCantidadObjetivo(recuDto.getCantidadObjetivo());
            necesidad.setCantidadRecibida(0);

            // Setear atributos propios de la recurrente
            necesidad.setPeriodo(recuDto.getPeriodo());
            // Como recién se crea, el período arranca hoy
            necesidad.setFechaInicioPeriodo(java.time.LocalDate.now());

            return necesidad;
        }
        throw new IllegalArgumentException("Tipo de Necesidad desconocido");
    }

    // Para cuando devuelva la info al frontend con un GET
    private NecesidadResponseDTO toNecesidadDTO(Necesidad necesidad) {
        if (necesidad instanceof NecesidadExtraordinaria extra) {
            return NecesidadExtraordinariaResponseDTO.builder()

                    .descripcion(extra.getDescripcion())
                    .cantidadObjetivo((double) extra.getCantidadObjetivo())
                    .cantidadRecibida((double) extra.getCantidadRecibida())
                    .satisfecha(extra.estaSatisfecha())
                    .build();

        } else if (necesidad instanceof NecesidadRecurrente recu) {
            return NecesidadRecurrenteResponseDTO.builder()

                    .descripcion(recu.getDescripcion())
                    .cantidadObjetivo((double) recu.getCantidadObjetivo())
                    .cantidadRecibida((double) recu.getCantidadRecibida())
                    .satisfecha(recu.estaSatisfecha())
                    .periodo(recu.getPeriodo())
                    .fechaInicioPeriodo(recu.getFechaInicioPeriodo())
                    .build();
        }
        throw new IllegalArgumentException("Tipo de Necesidad desconocido");
    }
}