package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.interfaces.integracion.EstrategiaRuteoPort;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Strategy + Adapter: traduce el modelo interno al contrato del proveedor
 * externo de ruteo y dispara la solicitud. El proveedor procesa de forma
 * asíncrona y devuelve el resultado a callbackUrl
 * (POST /api/logistica/planificaciones/callback).
 *
 * Mismo estilo "fire and forget con logging" que N8nWebhookClient en
 * servicio-incentivos: si el proveedor no responde al disparo inicial,
 * se loguea pero no se frena el flujo (el lote queda en ENVIADO y puede
 * reintentarse).
 */
@Component
public class ProveedorRuteoExternoAdapter implements EstrategiaRuteoPort {

    private static final Logger log = LoggerFactory.getLogger(ProveedorRuteoExternoAdapter.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String proveedorUrl;
    private final String callbackUrl;

    public ProveedorRuteoExternoAdapter(
            @Value("${integraciones.proveedor-ruteo.url}") String proveedorUrl,
            @Value("${servicio-logistica.base-url}") String baseUrl,
            ObjectMapper objectMapper) {
        this.proveedorUrl = proveedorUrl;
        this.callbackUrl = baseUrl + "/api/logistica/planificaciones/callback";
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void solicitarPlanificacion(LotePlanificacion lote, List<Camion> camiones) {
        try {
            Map<String, Object> payload = Map.of(
                    "loteId", lote.getId().toString(),
                    "tokenCorrelacion", lote.getTokenCorrelacion(),
                    "callbackUrl", callbackUrl,
                    "camiones", camiones.stream().map(this::camionAPayload).toList(),
                    "donaciones", lote.getDonaciones().stream().map(this::donacionAPayload).toList()
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(proveedorUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            log.info("[ProveedorRuteoExternoAdapter] Lote {} enviado al proveedor externo ({} donaciones, {} camiones)",
                    lote.getId(), lote.getDonaciones().size(), camiones.size());
        } catch (Exception e) {
            log.error("[ProveedorRuteoExternoAdapter] No se pudo enviar el lote {} al proveedor externo: {}",
                    lote.getId(), e.getMessage());
        }
    }

    private Map<String, Object> camionAPayload(Camion camion) {
        return Map.of(
                "id", camion.getId().toString(),
                "patente", camion.getPatente(),
                "capacidadVolumenM3", camion.getCapacidadVolumenM3(),
                "alturaM", camion.getAlturaM(),
                "capacidadCargaKg", camion.getCapacidadCargaKg()
        );
    }

    private Map<String, Object> donacionAPayload(DonacionLote donacion) {
        return Map.of(
                "idDonacion", donacion.getIdDonacion().toString(),
                "idEntidadBeneficiaria", donacion.getIdEntidadBeneficiaria().toString(),
                "direccionEntrega", donacion.getDireccionEntrega()
        );
    }
}
