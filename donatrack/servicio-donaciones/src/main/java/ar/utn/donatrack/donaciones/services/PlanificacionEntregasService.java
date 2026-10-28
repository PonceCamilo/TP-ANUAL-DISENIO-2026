package ar.utn.donatrack.donaciones.services;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Set;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.ArrayList;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.models.donacion.ProveedorLogistica;
import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.dtos.response.EnvioLogisticaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.DonacionNoDespachableException;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.LogisticaNoDisponibleException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ASIGNACION_REALIZADA;

/**
 * Deriva las donaciones asignadas al broker de logística para que les asigne un
 * proveedor y las incluya en una ruta.
 *
 *   matchmaking (03:30) ─▶ el administrador confirma el destino
 *                          ─▶ POST /donaciones/envios  (o el cron de las 04:00)
 *                             ─▶ broker (8087) ─▶ DONATRACK (8085) o EXTERNA (8086)
 *
 * Donaciones no conoce a los proveedores ni los elige: eso es responsabilidad del
 * broker, que además traduce los eventos de vuelta al formato de los callbacks de
 * /logistica/eventos/*.
 *
 * Hay dos formas de dispararlo a propósito:
 *   - El endpoint, para despachar un lote concreto a demanda (lo que pide el
 *     equipo de logística, porque las rutas se planifican por lotes).
 *   - El cron nocturno, que cumple el "en horarios de baja carga" del enunciado
 *     barriendo todo lo que haya quedado sin despachar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanificacionEntregasService {

    private final RepositorioDonaciones donacionesRepositorio;
    private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
    private final BrokerLogisticaClient brokerClient;
    private final DonacionesProperties propiedades;

    /**
     * El despacho usa dos transacciones cortas y deja afuera la llamada al
     * broker: no tiene sentido tener una conexión a la base abierta mientras se
     * espera a un servicio externo.
     */
    private final TransactionOperations transaccion;

    // ── Despacho a demanda (endpoint) ──────────────────────────────────────────

    /**
     * Despacha las donaciones indicadas.
     *
     * Valida TODO antes de llamar al broker: si alguna donación no existe, no está
     * en ASIGNACION_REALIZADA o su entidad no tiene dirección, falla sin despachar
     * nada. Es deliberado: un lote a medias dejaría parte de las donaciones en
     * logística y parte sin despachar, sin forma simple de reconciliarlas.
     *
     * Las donaciones y sus entidades se leen en dos consultas, no dos o tres por
     * donación.
     *
     * @param proveedor uno para forzarlo, o null para que el broker elija por
     *                  prioridad con fallback.
     */
    public EnvioLogisticaResponseDTO despachar(List<UUID> idsDonaciones, ProveedorLogistica proveedor) {
        List<BrokerLogisticaClient.DonacionEnvio> pedido = transaccion.execute(estado -> armarPedido(idsDonaciones));

        BrokerLogisticaClient.ResultadoEnvio resultado = llamarAlBroker(proveedor, pedido);

        transaccion.executeWithoutResult(estado -> registrarDespacho(idsDonaciones, resultado));

        log.info("[LOGISTICA] {} donaciones despachadas por '{}'. Proveedores descartados: {}",
                resultado.envios().size(), resultado.proveedor(), resultado.proveedoresDescartados());

        return EnvioLogisticaResponseDTO.desde(resultado);
    }

    // ── Barrido nocturno ───────────────────────────────────────────────────────

    /**
     * Despacha todo lo que haya quedado en ASIGNACION_REALIZADA. A diferencia del
     * endpoint, acá no se valida para fallar: las donaciones que no están en
     * condiciones simplemente se saltean, porque es un proceso desatendido y no
     * puede cortarse por un caso puntual.
     *
     * Recorre las donaciones de a lotes (donaciones.procesos.tamanio-lote) y
     * manda cada lote al broker por separado: antes las leía todas juntas y las
     * mandaba en un solo pedido, así que un error del broker dejaba sin despachar
     * todo el día. Ahora el lote que falla espera a la noche siguiente y los
     * demás salen igual.
     */
    @Scheduled(cron = "${donaciones.procesos.cron-despacho:0 0 4 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @SchedulerLock(name = "despacho-nocturno", lockAtMostFor = "PT1H")
    public void despacharPendientesDelDia() {
        int tamanio = propiedades.procesos().tamanioLote();
        int leidas = 0;
        int despachadas = 0;
        UUID ultima = null;
        LoteDeDespacho lote;
        do {
            UUID desde = ultima;
            lote = Objects.requireNonNull(transaccion.execute(estado -> despachablesDelLote(desde, tamanio)));
            leidas += lote.leidas();
            despachadas += despacharLote(lote.despachables());
            ultima = lote.ultima();
        } while (lote.leidas() == tamanio);

        log.info("[LOGISTICA] Barrido nocturno terminado. Con asignación realizada: {} | Despachadas: {}",
                leidas, despachadas);
    }

    /** Las del lote cuya entidad tiene una dirección a la que se pueda mandar el envío. */
    private LoteDeDespacho despachablesDelLote(UUID desde, int tamanio) {
        List<Donacion> asignadas = donacionesRepositorio.obtenerLoteEnEstado(ASIGNACION_REALIZADA, desde, tamanio);
        if (asignadas.isEmpty()) {
            return new LoteDeDespacho(List.of(), 0, desde);
        }
        Map<UUID, EntidadBeneficiaria> entidades = entidadesDe(asignadas);
        List<UUID> despachables = asignadas.stream()
                .filter(donacion -> tieneDireccionResoluble(entidades.get(donacion.getIdEntidadBeneficiaria())))
                .map(Donacion::getId)
                .toList();
        return new LoteDeDespacho(despachables, asignadas.size(), asignadas.getLast().getId());
    }

    /** Despacha un lote; si el broker falla, sus donaciones siguen en ASIGNACION_REALIZADA y entran mañana. */
    private int despacharLote(List<UUID> despachables) {
        if (despachables.isEmpty()) {
            return 0;
        }
        try {
            // proveedor null: que el broker elija y haga fallback.
            return despachar(despachables, null).getEnvios().size();
        } catch (Exception e) {
            log.error("[LOGISTICA] El barrido nocturno no pudo despachar un lote de {} donaciones: {}",
                    despachables.size(), e.getMessage());
            return 0;
        }
    }

    /** Lo que deja un lote: las donaciones a despachar, cuántas leyó y la última, que es donde sigue el próximo. */
    private record LoteDeDespacho(List<UUID> despachables, int leidas, UUID ultima) {
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Marca como despachadas las donaciones que el broker aceptó, todas en una transacción. */
    private void registrarDespacho(List<UUID> pedidas, BrokerLogisticaClient.ResultadoEnvio resultado) {
        Map<UUID, Donacion> donaciones = donacionesRepositorio.obtenerPorIds(Set.copyOf(pedidas)).stream()
                .collect(Collectors.toMap(Donacion::getId, Function.identity()));
        for (BrokerLogisticaClient.EnvioAsignado envio : resultado.envios()) {
            Donacion donacion = donaciones.get(envio.idDonacion());
            if (donacion == null) {
                log.warn("[LOGISTICA] El broker devolvió un envío para la donación {}, que no estaba en el pedido.",
                        envio.idDonacion());
                continue;
            }
            donacion.despacharA(ProveedorLogistica.desde(envio.proveedor()), envio.idSeguimiento());
            donacionesRepositorio.guardar(donacion);
        }
    }

    /**
     * Recupera las donaciones y verifica que todas estén en condiciones de
     * despacharse, y las traduce al pedido del broker en el orden pedido.
     */
    private List<BrokerLogisticaClient.DonacionEnvio> armarPedido(List<UUID> idsDonaciones) {
        if (idsDonaciones == null || idsDonaciones.isEmpty()) {
            throw new DonacionNoDespachableException("Hay que indicar al menos una donación para despachar.");
        }
        Set<UUID> pedidas = new LinkedHashSet<>(idsDonaciones);
        Map<UUID, Donacion> donaciones = donacionesRepositorio.obtenerPorIds(pedidas).stream()
                .collect(Collectors.toMap(Donacion::getId, Function.identity()));
        Map<UUID, EntidadBeneficiaria> entidades = entidadesDe(donaciones.values());

        List<BrokerLogisticaClient.DonacionEnvio> pedido = new ArrayList<>();
        for (UUID id : pedidas) {
            Donacion donacion = donaciones.get(id);
            if (donacion == null) {
                throw new DonacionNoEncontradaException(id);
            }
            if (!donacion.estaEnEstado(ASIGNACION_REALIZADA)) {
                throw new DonacionNoDespachableException(
                        "La donación " + id + " está en " + donacion.getEstado().nombre()
                                + " y solo se puede despachar desde " + ASIGNACION_REALIZADA + ".");
            }
            EntidadBeneficiaria entidad = entidades.get(donacion.getIdEntidadBeneficiaria());
            if (!tieneDireccionResoluble(entidad)) {
                throw new DonacionNoDespachableException(
                        "La donación " + id + " no tiene una entidad beneficiaria con dirección cargada.");
            }
            pedido.add(aDonacionEnvio(donacion, entidad));
        }
        return pedido;
    }

    /** Las entidades de esas donaciones, en una sola consulta. */
    private Map<UUID, EntidadBeneficiaria> entidadesDe(Collection<Donacion> donaciones) {
        Set<UUID> ids = donaciones.stream()
                .map(Donacion::getIdEntidadBeneficiaria)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return entidadesRepositorio.obtenerPorIds(ids).stream()
                .collect(Collectors.toMap(EntidadBeneficiaria::getId, Function.identity()));
    }

    private static boolean tieneDireccionResoluble(EntidadBeneficiaria entidad) {
        return entidad != null
                && entidad.getDireccion() != null
                && entidad.getDireccion().getLocalidad() != null
                && entidad.getDireccion().getLocalidad().getProvincia() != null;
    }

    /**
     * Traduce la donación al contrato del broker. La dirección de donaciones anida
     * Localidad y Provincia; la del broker es plana, así que hay que aplanarla.
     */
    private BrokerLogisticaClient.DonacionEnvio aDonacionEnvio(Donacion donacion, EntidadBeneficiaria entidad) {
        Direccion direccion = entidad.getDireccion();

        return new BrokerLogisticaClient.DonacionEnvio(
                donacion.getId(),
                entidad.getId(),
                entidad.getRazonSocial(),
                new BrokerLogisticaClient.DireccionEnvio(
                        direccion.getCalle(),
                        direccion.getNumero(),
                        direccion.getLocalidad().getNombre(),
                        direccion.getLocalidad().getProvincia().getNombre(),
                        direccion.getCodigoPostal()));
    }

    /**
     * Aísla los errores del broker en una excepción de dominio.
     *
     * El caso esperable es el 503 (ningún proveedor disponible) y que el broker
     * no responda. En los dos casos NO se toca el estado de las donaciones: se
     * reintenta más tarde.
     */
    private BrokerLogisticaClient.ResultadoEnvio llamarAlBroker(
            ProveedorLogistica proveedor, List<BrokerLogisticaClient.DonacionEnvio> pedido) {
        try {
            BrokerLogisticaClient.ResultadoEnvio resultado = brokerClient.solicitarEnvio(
                    proveedor == null ? null : proveedor.name(), pedido);
            if (resultado == null || resultado.envios() == null) {
                throw new LogisticaNoDisponibleException("El broker respondió sin envíos asignados.");
            }
            return resultado;

        } catch (LogisticaNoDisponibleException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LOGISTICA] El broker no pudo despachar el pedido: {}", e.getMessage());
            throw new LogisticaNoDisponibleException(
                    "No se pudo despachar a logística: " + e.getMessage());
        }
    }
}
