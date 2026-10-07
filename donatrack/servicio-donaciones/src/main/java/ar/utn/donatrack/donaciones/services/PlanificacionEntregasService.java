package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.dtos.response.EnvioLogisticaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.DonacionNoDespachableException;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.LogisticaNoDisponibleException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Único estado desde el que una donación puede despacharse a logística. */
    private static final String ESTADO_ASIGNADA = "ASIGNACION_REALIZADA";

    private final DonacionesRepositoryInterface donacionesRepositorio;
    private final EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;
    private final BrokerLogisticaClient brokerClient;
    private final DonacionesValidator validador;

    // ── Despacho a demanda (endpoint) ──────────────────────────────────────────

    /**
     * Despacha las donaciones indicadas.
     *
     * Valida TODO antes de llamar al broker: si alguna donación no existe, no está
     * en ASIGNACION_REALIZADA o su entidad no tiene dirección, falla sin despachar
     * nada. Es deliberado: un lote a medias dejaría parte de las donaciones en
     * logística y parte sin despachar, sin forma simple de reconciliarlas.
     *
     * @param proveedor "DONATRACK"/"EXTERNA" para forzar uno, o null para que el
     *                  broker elija por prioridad con fallback.
     */
    public EnvioLogisticaResponseDTO despachar(List<UUID> idsDonaciones, String proveedor) {
        Map<UUID, Donacion> donaciones = validarYObtener(idsDonaciones);

        List<BrokerLogisticaClient.DonacionEnvio> pedido = donaciones.values().stream()
                .map(this::aDonacionEnvio)
                .toList();

        BrokerLogisticaClient.ResultadoEnvio resultado = llamarAlBroker(proveedor, pedido);

        for (BrokerLogisticaClient.EnvioAsignado envio : resultado.envios()) {
            Donacion donacion = donaciones.get(envio.idDonacion());
            if (donacion == null) {
                log.warn("[LOGISTICA] El broker devolvió un envío para la donación {}, que no estaba en el pedido.",
                        envio.idDonacion());
                continue;
            }
            donacion.despacharA(envio.proveedor(), envio.idSeguimiento());
            donacionesRepositorio.guardar(donacion);
        }

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
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void despacharPendientesDelDia() {
        List<Donacion> asignadas = donacionesRepositorio.obtenerPorEstado(ESTADO_ASIGNADA);
        log.info("[LOGISTICA] Barrido nocturno. Donaciones con asignación realizada: {}", asignadas.size());

        List<UUID> despachables = asignadas.stream()
                .filter(this::tieneDireccionResoluble)
                .map(Donacion::getId)
                .toList();

        if (despachables.isEmpty()) {
            log.info("[LOGISTICA] No hay donaciones en condiciones de despacharse.");
            return;
        }

        try {
            // proveedor null: que el broker elija y haga fallback.
            despachar(despachables, null);
        } catch (Exception e) {
            // Las donaciones siguen en ASIGNACION_REALIZADA y entran mañana otra vez.
            log.error("[LOGISTICA] El barrido nocturno no pudo despachar: {}", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Recupera las donaciones y verifica que todas estén en condiciones de
     * despacharse. Devuelve un mapa por id para poder casar la respuesta del
     * broker sin volver a recorrer la lista.
     */
    private Map<UUID, Donacion> validarYObtener(List<UUID> idsDonaciones) {
        if (idsDonaciones == null || idsDonaciones.isEmpty()) {
            throw new DonacionNoDespachableException("Hay que indicar al menos una donación para despachar.");
        }

        Map<UUID, Donacion> resultado = new HashMap<>();
        for (UUID id : idsDonaciones) {
            // Lanza DonacionNoEncontradaException (404) si no existe.
            Donacion donacion = validador.validarYObtenerDonacion(id);

            if (!donacion.estaEnEstado(ESTADO_ASIGNADA)) {
                throw new DonacionNoDespachableException(
                        "La donación " + id + " está en " + donacion.getEstado().nombre()
                                + " y solo se puede despachar desde " + ESTADO_ASIGNADA + ".");
            }
            if (!tieneDireccionResoluble(donacion)) {
                throw new DonacionNoDespachableException(
                        "La donación " + id + " no tiene una entidad beneficiaria con dirección cargada.");
            }
            resultado.put(id, donacion);
        }
        return resultado;
    }

    private boolean tieneDireccionResoluble(Donacion donacion) {
        EntidadBeneficiaria entidad = entidadDe(donacion);
        return entidad != null
                && entidad.getDireccion() != null
                && entidad.getDireccion().getLocalidad() != null
                && entidad.getDireccion().getLocalidad().getProvincia() != null;
    }

    private EntidadBeneficiaria entidadDe(Donacion donacion) {
        UUID idEntidad = donacion.getIdEntidadBeneficiaria();
        return idEntidad == null ? null : entidadesRepositorio.obtenerPorId(idEntidad);
    }

    /**
     * Traduce la donación al contrato del broker. La dirección de donaciones anida
     * Localidad y Provincia; la del broker es plana, así que hay que aplanarla.
     */
    private BrokerLogisticaClient.DonacionEnvio aDonacionEnvio(Donacion donacion) {
        EntidadBeneficiaria entidad = entidadDe(donacion);
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
            String proveedor, List<BrokerLogisticaClient.DonacionEnvio> pedido) {
        try {
            BrokerLogisticaClient.ResultadoEnvio resultado = brokerClient.solicitarEnvio(proveedor, pedido);
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
