package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.dtos.request.*;
import ar.utn.donatrack.donaciones.dtos.response.*;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.exceptions.comunes.TipoDesconocidoException;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntidadBeneficiariaMapper {

    private final DireccionMapper direcciones;
    private final ContactoMapper contactos;
    private final RepresentanteMapper representantes;

    // ── ENTIDAD ─────────────────────────────────────────────────────────────

    public EntidadBeneficiaria toModel(EntidadBeneficiariaRequestDTO dto) {
        return EntidadBeneficiaria.builder()
                .razonSocial(dto.getRazonSocial())
                .direccion(direcciones.toDireccion(dto.getDireccion()))
                .contactos(contactos.toContactos(dto.getContactos()))
                .representantes(representantes.toRepresentantes(dto.getRepresentantes()))
                .campanias(new ArrayList<>())
                .build();
    }

    public EntidadBeneficiariaResponseDTO toDTO(EntidadBeneficiaria entidad) {
        return EntidadBeneficiariaResponseDTO.builder()
                .id(entidad.getId())
                .razonSocial(entidad.getRazonSocial())
                .direccion(direcciones.toDireccionDTO(entidad.getDireccion()))
                .contactos(contactos.toContactosDTO(entidad.getContactos()))
                .representantes(representantes.toRepresentantesDTO(entidad.getRepresentantes()))
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

    public CampaniaResponseDTO toCampaniaDTO(Campania campania) {
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

    /**
     * Lo recibido y la satisfacción se informan a hoy: en una recurrente cuyo
     * período ya terminó, lo recibido del período anterior no cuenta.
     */
    public NecesidadResponseDTO toNecesidadDTO(Necesidad necesidad) {
        LocalDate hoy = FechaHoraArgentina.hoy();
        if (necesidad instanceof NecesidadExtraordinaria extra) {
            return NecesidadExtraordinariaResponseDTO.builder()
                    .id(extra.getId())
                    .nombre(extra.getNombre())
                    .descripcion(extra.getDescripcion())
                    .subcategoria(textoDe(extra.getSubcategoria()))
                    .cantidadObjetivo(extra.getCantidadObjetivo())
                    .cantidadRecibida(extra.cantidadRecibidaAl(hoy))
                    .satisfecha(extra.estaSatisfechaAl(hoy))
                    .build();

        } else if (necesidad instanceof NecesidadRecurrente recu) {
            return NecesidadRecurrenteResponseDTO.builder()
                    .id(recu.getId())
                    .nombre(recu.getNombre())
                    .descripcion(recu.getDescripcion())
                    .subcategoria(textoDe(recu.getSubcategoria()))
                    .cantidadObjetivo(recu.getCantidadObjetivo())
                    .cantidadRecibida(recu.cantidadRecibidaAl(hoy))
                    .satisfecha(recu.estaSatisfechaAl(hoy))
                    .periodo(recu.getPeriodo())
                    .fechaInicioPeriodo(recu.getFechaInicioPeriodo())
                    .build();
        }
        throw new TipoDesconocidoException("necesidad");
    }

    private static String textoDe(Subcategoria subcategoria) {
        return subcategoria == null ? null : subcategoria.getTipo();
    }
}