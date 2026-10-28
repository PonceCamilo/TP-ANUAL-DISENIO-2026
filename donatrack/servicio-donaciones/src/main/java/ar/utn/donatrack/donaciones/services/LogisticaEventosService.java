package ar.utn.donatrack.donaciones.services;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Set;
import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGA_FALLIDA;

/**
 * Recibe los callbacks de eventos logísticos y dispara las notificaciones
 * correspondientes hacia el Servicio de Notificaciones.
 *
 * Regla arquitectónica (Entrega 3): el Servicio de Logística NO llama
 * directamente a notificaciones ni a incentivos; deja disponible la
 * información y es el Servicio de Donaciones quien orquesta las notificaciones.
 *
 * Cada callback es una transacción: si una donación de la ruta falla, no queda
 * ninguna a medias (y los avisos, que van a la bandeja de salida, tampoco). Los
 * callbacks son idempotentes: un aviso repetido responde bien sin cambiar nada
 * ni volver a notificar.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class LogisticaEventosService {

    private final RepositorioDonaciones donacionesRepositorio;
    private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
    private final RepositorioPersonasDonantes donanteRepositorio;
    private final Notificador notificador;

    // ── INICIO DE RUTA ─────────────────────────────────────────────────────────

    /**
     * El chofer inició el recorrido. Se notifica a todas las entidades
     * beneficiarias y donantes cuyas donaciones forman parte de la ruta,
     * incluyendo el link al mapa interactivo de seguimiento en tiempo real.
     *
     * Las donaciones, sus donantes y sus entidades se leen en tres consultas en
     * total, no tres por donación.
     */
    public void procesarInicioRuta(InicioRutaCallbackDTO dto) {
        log.info("[LOGISTICA] Inicio de ruta {}. Donaciones involucradas: {}", dto.idRuta(), dto.idsDonaciones().size());

        List<Donacion> donaciones = obtenerTodasOFallar(dto.idsDonaciones());
        Set<UUID> idsDonantes = ids(donaciones, Donacion::getIdDonante);
        Set<UUID> idsEntidades = ids(donaciones, Donacion::getIdEntidadBeneficiaria);
        Map<UUID, PersonaDonante> donantes = idsDonantes.isEmpty() ? Map.of()
                : porId(donanteRepositorio.obtenerPorIds(idsDonantes), PersonaDonante::getId);
        Map<UUID, EntidadBeneficiaria> entidades = idsEntidades.isEmpty() ? Map.of()
                : porId(entidadesRepositorio.obtenerPorIds(idsEntidades), EntidadBeneficiaria::getId);

        for (Donacion donacion : donaciones) {
            if (!donacion.iniciarTraslado("Chofer inició el recorrido. Ruta: " + dto.idRuta())) {
                log.info("[LOGISTICA] Inicio de ruta repetido para la donación {}: sin cambios.", donacion.getId());
                continue;
            }
            donacionesRepositorio.guardar(donacion);
            notificador.inicioDeRuta(de(donantes, donacion.getIdDonante()),
                    de(entidades, donacion.getIdEntidadBeneficiaria()), dto.urlMapaInteractivo());
        }
    }

    // ── ENTREGA EXITOSA ────────────────────────────────────────────────────────

    /**
     * La entidad beneficiaria confirmó la recepción. Se actualiza el estado
     * de la donación a ENTREGADA y se notifica a entidad y donante con el
     * comprobante que incluye fecha, hora y patente del camión.
     */
    public void procesarEntregaExitosa(EntregaExitosaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega exitosa confirmada para donación {}", dto.idDonacion());

        Donacion donacion = obtenerDonacionOFallar(dto.idDonacion());

        // La entrega queda con la fecha que informa logística. Imputar lo entregado
        // a las necesidades y avisar a incentivos lo hacen los oyentes del evento.
        if (!donacion.registrarEntrega(dto.fechaHoraEntrega(),
                "Entrega confirmada. Camión: " + dto.patenteCamion() + " | Fecha: " + dto.fechaHoraEntrega())) {
            log.info("[LOGISTICA] Entrega exitosa repetida para la donación {}: sin cambios.", donacion.getId());
            return;
        }
        donacionesRepositorio.guardar(donacion);

        notificador.entregaExitosa(donanteDe(donacion), entidadDe(donacion), armarComprobante(dto));
    }

    private String armarComprobante(EntregaExitosaCallbackDTO dto) {
        return "Comprobante de entrega — Camión: " + dto.patenteCamion()
                + " | Fecha y hora: " + dto.fechaHoraEntrega();
    }

    // ── ENTREGA FALLIDA ────────────────────────────────────────────────────────

    /**
     * La entrega no pudo concretarse. Se actualiza el estado a ENTREGA_FALLIDA
     * y se notifica a la entidad, al donante y al administrador del sistema.
     * Si la donación es replanificable, queda registrado en el historial.
     */
    public void procesarEntregaFallida(EntregaFallidaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega fallida para donación {}. Motivo: {}", dto.idDonacion(), dto.motivoFallo());

        Donacion donacion = obtenerDonacionOFallar(dto.idDonacion());

        String justificacion = dto.motivoFallo()
                + (dto.replanificable() ? " | Puede ser replanificada." : " | No puede ser replanificada.");

        if (!donacion.registrarEntregaFallida(justificacion)) {
            log.info("[LOGISTICA] Entrega fallida repetida para la donación {}: sin cambios.", donacion.getId());
            return;
        }
        donacionesRepositorio.guardar(donacion);

        notificador.entregaFallida(donacion.getId(), donanteDe(donacion), entidadDe(donacion),
                dto.motivoFallo(), dto.replanificable());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Donacion obtenerDonacionOFallar(UUID id) {
        return donacionesRepositorio.buscarPorId(id).orElseThrow(() -> new DonacionNoEncontradaException(id));
    }

    /** Las donaciones de la ruta, en el orden del aviso y sin repetir; 404 si falta alguna. */
    private List<Donacion> obtenerTodasOFallar(List<UUID> ids) {
        Set<UUID> pedidas = new LinkedHashSet<>(ids);
        Map<UUID, Donacion> encontradas = porId(donacionesRepositorio.obtenerPorIds(pedidas), Donacion::getId);
        List<Donacion> donaciones = new ArrayList<>();
        for (UUID id : pedidas) {
            Donacion donacion = encontradas.get(id);
            if (donacion == null) {
                throw new DonacionNoEncontradaException(id);
            }
            donaciones.add(donacion);
        }
        return donaciones;
    }

    private static Set<UUID> ids(List<Donacion> donaciones, Function<Donacion, UUID> id) {
        return donaciones.stream().map(id).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** El elemento de ese id, o null si no hay id (una donación sin entidad asignada). */
    private static <T> T de(Map<UUID, T> elementos, UUID id) {
        return id == null ? null : elementos.get(id);
    }

    private static <T> Map<UUID, T> porId(List<T> elementos, Function<T, UUID> id) {
        return elementos.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }

    private PersonaDonante donanteDe(Donacion donacion) {
        return donanteRepositorio.buscarPorId(donacion.getIdDonante()).orElse(null);
    }

    private EntidadBeneficiaria entidadDe(Donacion donacion) {
        UUID idEntidad = donacion.getIdEntidadBeneficiaria();
        return idEntidad == null ? null : entidadesRepositorio.buscarPorId(idEntidad).orElse(null);
    }
}
