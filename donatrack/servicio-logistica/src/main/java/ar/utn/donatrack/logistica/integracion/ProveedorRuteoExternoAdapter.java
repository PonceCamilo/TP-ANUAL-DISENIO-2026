package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import ar.utn.donatrack.logistica.exceptions.ProveedorRuteoIndisponibleException;
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
 * externo de ruteo y pide, de a un camión por vez, que planifique su ruta.
 * El proveedor procesa esa solicitud en el momento y devuelve la ruta
 * planificada como response HTTP síncrona (no hay callback de por medio
 * para este flujo).
 */
@Component
public class ProveedorRuteoExternoAdapter implements EstrategiaRuteoPort {

    private static final Logger log = LoggerFactory.getLogger(ProveedorRuteoExternoAdapter.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String proveedorUrl;

    public ProveedorRuteoExternoAdapter(
            @Value("${integraciones.proveedor-ruteo.url}") String proveedorUrl,
            ObjectMapper objectMapper) {
        this.proveedorUrl = proveedorUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public RutaPlanificadaProveedorDTO planificarParaCamion(LotePlanificacion lote, Camion camion, List<DonacionLote> donaciones) {
        try {
            Map<String, Object> payload = Map.of(
                    "loteId", lote.getId().toString(),
                    "camion", camionAPayload(camion),
                    "donaciones", donaciones.stream().map(this::donacionAPayload).toList()
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(proveedorUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            RutaPlanificadaProveedorDTO rutaPlanificada = objectMapper.readValue(response.body(), RutaPlanificadaProveedorDTO.class);

            log.info("[ProveedorRuteoExternoAdapter] Lote {} - camión {} planificado ({} donaciones -> {} paradas)",
                    lote.getId(), camion.getId(), donaciones.size(), rutaPlanificada.getParadas().size());

            return rutaPlanificada;
        } catch (Exception e) {
            log.error("[ProveedorRuteoExternoAdapter] No se pudo planificar el camión {} del lote {}: {}",
                    camion.getId(), lote.getId(), e.getMessage());
            throw new ProveedorRuteoIndisponibleException(camion.getId(), e);
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
