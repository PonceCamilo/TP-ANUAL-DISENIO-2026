package ar.utn.donatrack.donaciones.integracion.logistica;

import ar.utn.donatrack.donaciones.interfaces.integracion.LogisticaPort;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter del servicio de logística propio del sistema (puerto 8085).
 *
 * Traduce el lenguaje de donaciones al contrato de ese servicio:
 *   1. consulta la flota y se queda con los camiones DISPONIBLE;
 *   2. arma el payload de POST /api/logistica/planificaciones;
 *   3. lo envía y reporta si fue aceptado.
 *
 * POR QUÉ CONSULTA LOS CAMIONES ACÁ: el contrato de logística exige la lista de
 * camionesIds, pero la flota es un concepto de logística, no de donaciones.
 * Resolverlo dentro del adapter mantiene el puerto limpio: quien dispara la
 * planificación solo entrega donaciones, sin saber que existen camiones.
 */
@Component
public class LogisticaPropiaAdapter implements LogisticaPort {

    private static final Logger log = LoggerFactory.getLogger(LogisticaPropiaAdapter.class);

    private static final String NOMBRE = "propia";
    private static final String ESTADO_DISPONIBLE = "DISPONIBLE";

    private final RestClient restClient;

    public LogisticaPropiaAdapter(@Value("${logistica.propia.url:http://localhost:8085}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public String nombre() {
        return NOMBRE;
    }

    /** Está disponible si responde la consulta de flota y hay al menos un camión libre. */
    public boolean estaDisponible() {
        try {
            return !camionesDisponibles().isEmpty();
        } catch (Exception e) {
            log.warn("[Logistica/propia] No responde: {}", e.getMessage());
            return false;
        }
    }

    public ResultadoPlanificacion solicitarPlanificacion(List<SolicitudEntrega> entregas) {
        if (entregas.isEmpty()) {
            return ResultadoPlanificacion.rechazada(NOMBRE, "No había donaciones para planificar.");
        }

        try {
            List<UUID> camiones = camionesDisponibles();
            if (camiones.isEmpty()) {
                return ResultadoPlanificacion.rechazada(NOMBRE, "No hay camiones disponibles en la flota.");
            }

            restClient.post()
                    .uri("/api/logistica/planificaciones")
                    .body(Map.of(
                            "donaciones", entregas.stream().map(this::aDonacionParaRutear).toList(),
                            "camionesIds", camiones))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Logistica/propia] {} donaciones derivadas a planificar con {} camiones.",
                    entregas.size(), camiones.size());

            return ResultadoPlanificacion.aceptada(NOMBRE,
                    entregas.stream().map(SolicitudEntrega::idDonacion).toList());

        } catch (Exception e) {
            log.error("[Logistica/propia] Falló la solicitud de planificación: {}", e.getMessage());
            return ResultadoPlanificacion.rechazada(NOMBRE, "Error al solicitar la planificación: " + e.getMessage());
        }
    }

    /** Ids de los camiones de la flota que están en condiciones de salir a repartir. */
    private List<UUID> camionesDisponibles() {
        List<Map<String, Object>> flota = restClient.get()
                .uri("/api/logistica/camiones")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});

        if (flota == null) {
            return List.of();
        }
        return flota.stream()
                .filter(camion -> ESTADO_DISPONIBLE.equals(String.valueOf(camion.get("estado"))))
                .map(camion -> UUID.fromString(String.valueOf(camion.get("id"))))
                .toList();
    }

    /** Traduce al DonacionParaRutearRequestDTO que espera el servicio de logística. */
    private Map<String, Object> aDonacionParaRutear(SolicitudEntrega entrega) {
        return Map.of(
                "idDonacion", entrega.idDonacion(),
                "idEntidadBeneficiaria", entrega.idEntidadBeneficiaria(),
                "direccionEntrega", aDireccion(entrega.direccionEntrega()));
    }

    /**
     * La dirección de logística es plana (calle, numero, localidad, provincia,
     * codigoPostal) mientras que la de donaciones anida Localidad y Provincia.
     * El aplanado es parte de la traducción que corresponde al adapter.
     */
    private Map<String, Object> aDireccion(Direccion direccion) {
        String localidad = direccion != null && direccion.getLocalidad() != null
                ? direccion.getLocalidad().getNombre() : "";
        String provincia = direccion != null && direccion.getLocalidad() != null
                && direccion.getLocalidad().getProvincia() != null
                ? direccion.getLocalidad().getProvincia().getNombre() : "";

        return Map.of(
                "calle", direccion != null && direccion.getCalle() != null ? direccion.getCalle() : "",
                "numero", direccion != null ? direccion.getNumero() : 0,
                "localidad", localidad,
                "provincia", provincia,
                "codigoPostal", direccion != null && direccion.getCodigoPostal() != null
                        ? direccion.getCodigoPostal() : "");
    }
}
