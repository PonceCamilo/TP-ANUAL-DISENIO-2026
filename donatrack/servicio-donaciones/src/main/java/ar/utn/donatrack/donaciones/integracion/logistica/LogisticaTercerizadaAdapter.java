package ar.utn.donatrack.donaciones.integracion.logistica;

import ar.utn.donatrack.donaciones.interfaces.integracion.LogisticaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Adapter del "otro servicio potencial que cumple igual objetivo" del que habla
 * la Entrega 4: un operador logístico tercerizado al que la ONG podría contratar.
 *
 * Es el segundo proveedor que le da sentido al broker. No existe un servicio real
 * detrás, igual que el MockProveedorRuteoController que ya usa logística para
 * simular a su proveedor externo de ruteo: lo que importa a nivel diseño es que
 * sea intercambiable con el propio sin que nadie más se entere.
 *
 * Su contrato es DISTINTO al del servicio propio a propósito (otro endpoint, otro
 * payload, y no necesita que le pasen camiones porque usa su propia flota). Si
 * ambos adapters hablaran igual, el puerto no estaría demostrando nada.
 *
 * Viene deshabilitado: se activa con `logistica.tercerizada.habilitado=true`.
 */
@Component
@ConditionalOnProperty(name = "logistica.tercerizada.habilitado", havingValue = "true")
public class LogisticaTercerizadaAdapter implements LogisticaPort {

    private static final Logger log = LoggerFactory.getLogger(LogisticaTercerizadaAdapter.class);

    private static final String NOMBRE = "tercerizada";

    private final RestClient restClient;

    public LogisticaTercerizadaAdapter(
            @Value("${logistica.tercerizada.url:http://localhost:9090}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public String nombre() {
        return NOMBRE;
    }

    public boolean estaDisponible() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("[Logistica/tercerizada] No responde: {}", e.getMessage());
            return false;
        }
    }

    public ResultadoPlanificacion solicitarPlanificacion(List<SolicitudEntrega> entregas) {
        if (entregas.isEmpty()) {
            return ResultadoPlanificacion.rechazada(NOMBRE, "No había donaciones para planificar.");
        }

        try {
            // Contrato propio del tercero: una lista plana de envíos, sin camiones.
            restClient.post()
                    .uri("/api/envios/lote")
                    .body(Map.of("envios", entregas.stream().map(this::aEnvio).toList()))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Logistica/tercerizada] {} donaciones derivadas al operador tercerizado.", entregas.size());

            return ResultadoPlanificacion.aceptada(NOMBRE,
                    entregas.stream().map(SolicitudEntrega::idDonacion).toList());

        } catch (Exception e) {
            log.error("[Logistica/tercerizada] Falló la solicitud: {}", e.getMessage());
            return ResultadoPlanificacion.rechazada(NOMBRE, "Error al solicitar la planificación: " + e.getMessage());
        }
    }

    /** El tercero identifica cada envío con una referencia externa y una dirección en una línea. */
    private Map<String, Object> aEnvio(SolicitudEntrega entrega) {
        return Map.of(
                "referenciaExterna", entrega.idDonacion().toString(),
                "destinatarioId", entrega.idEntidadBeneficiaria().toString(),
                "direccion", direccionEnUnaLinea(entrega));
    }

    private String direccionEnUnaLinea(SolicitudEntrega entrega) {
        var direccion = entrega.direccionEntrega();
        if (direccion == null) {
            return "";
        }
        String localidad = direccion.getLocalidad() != null ? direccion.getLocalidad().getNombre() : "";
        String provincia = direccion.getLocalidad() != null && direccion.getLocalidad().getProvincia() != null
                ? direccion.getLocalidad().getProvincia().getNombre() : "";

        return "%s %d, %s, %s".formatted(
                direccion.getCalle() != null ? direccion.getCalle() : "",
                direccion.getNumero(), localidad, provincia);
    }
}
