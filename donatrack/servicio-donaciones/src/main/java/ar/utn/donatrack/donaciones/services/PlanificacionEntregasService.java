package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.integracion.logistica.BrokerLogistica;
import ar.utn.donatrack.donaciones.integracion.logistica.ResultadoPlanificacion;
import ar.utn.donatrack.donaciones.integracion.logistica.SolicitudEntrega;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dispara la planificación de las entregas del día siguiente.
 *
 * Es el eslabón que faltaba en la cadena: hasta ahora nadie invocaba la
 * planificación de rutas, así que el endpoint de logística existía sin que
 * ningún proceso lo usara.
 *
 *   AsignacionBatchService (03:30) ─▶ el administrador confirma destinos
 *                                     ─▶ este servicio (04:00) ─▶ BrokerLogistica ─▶ proveedor
 *
 * Corre después del matchmaking para que alcance a tomar las donaciones que se
 * hayan confirmado, y en horario de baja carga como pide el enunciado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificacionEntregasService {

    /** Solo se derivan a logística las donaciones que ya tienen entidad destino. */
    private static final String ESTADO_ASIGNADA = "ASIGNACION_REALIZADA";

    private final DonacionesRepositoryInterface donacionesRepositorio;
    private final EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;
    private final BrokerLogistica broker;

    @Scheduled(cron = "0 0 4 * * *")
    public void planificarEntregasDelDiaSiguiente() {
        List<Donacion> asignadas = donacionesRepositorio.obtenerPorEstado(ESTADO_ASIGNADA);

        log.info("[PLANIFICACION] Inicio. Donaciones con asignación realizada: {}", asignadas.size());

        List<SolicitudEntrega> entregas = armarSolicitudes(asignadas);
        if (entregas.isEmpty()) {
            log.info("[PLANIFICACION] No hay donaciones en condiciones de ser planificadas.");
            return;
        }

        ResultadoPlanificacion resultado = broker.planificar(entregas);

        if (resultado.aceptada()) {
            marcarListasParaEntregar(asignadas, resultado.idsDonaciones());
            log.info("[PLANIFICACION] Finalizada por '{}': {}", resultado.proveedor(), resultado.detalle());
        } else {
            // No se cambia ningún estado: las donaciones siguen ASIGNACION_REALIZADA
            // y entran de nuevo en la corrida de mañana.
            log.error("[PLANIFICACION] No se pudo planificar: {}", resultado.detalle());
        }
    }

    /**
     * Arma una solicitud por donación, resolviendo la dirección de la entidad
     * destino. Las donaciones cuya entidad no se puede resolver se saltean: sin
     * dirección no hay ruta posible, y frenar todo el lote por una sería peor.
     */
    private List<SolicitudEntrega> armarSolicitudes(List<Donacion> asignadas) {
        List<SolicitudEntrega> solicitudes = new ArrayList<>();

        for (Donacion donacion : asignadas) {
            UUID idEntidad = donacion.getIdEntidadBeneficiaria();
            if (idEntidad == null) {
                log.warn("[PLANIFICACION] La donación {} está asignada pero sin entidad; se saltea.", donacion.getId());
                continue;
            }

            EntidadBeneficiaria entidad = entidadesRepositorio.obtenerPorId(idEntidad);
            if (entidad == null || entidad.getDireccion() == null) {
                log.warn("[PLANIFICACION] La entidad {} no existe o no tiene dirección; se saltea la donación {}.",
                        idEntidad, donacion.getId());
                continue;
            }

            solicitudes.add(new SolicitudEntrega(donacion.getId(), idEntidad, entidad.getDireccion()));
        }
        return solicitudes;
    }

    /**
     * Pasa a LISTA_PARA_ENTREGAR las donaciones que el proveedor aceptó.
     *
     * DECISIÓN DE DISEÑO: el enunciado dice que la donación "pasará a estar Lista
     * para entregar cuando se haya planificado una ruta que incluya la donación".
     * Lo correcto sería esperar a que logística confirme las rutas generadas, pero
     * hoy no existe un callback hacia donaciones para ese evento (los tres que hay
     * son inicio de ruta, entrega exitosa y entrega fallida). Hasta que exista, se
     * toma la aceptación del proveedor como la señal de planificación; si no, la
     * donación nunca saldría de ASIGNACION_REALIZADA y el inicio de ruta fallaría
     * con una transición ilegal.
     */
    private void marcarListasParaEntregar(List<Donacion> asignadas, List<UUID> idsAceptados) {
        for (Donacion donacion : asignadas) {
            if (!idsAceptados.contains(donacion.getId())) {
                continue;
            }
            try {
                donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar",
                        "Incluida en la planificación de rutas del día siguiente.");
            } catch (Exception e) {
                log.error("[PLANIFICACION] No se pudo marcar como lista la donación {}: {}",
                        donacion.getId(), e.getMessage());
            }
        }
    }
}
