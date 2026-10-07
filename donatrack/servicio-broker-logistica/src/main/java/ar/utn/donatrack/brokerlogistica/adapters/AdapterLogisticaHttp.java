package ar.utn.donatrack.brokerlogistica.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Base de los adapters que hablan HTTP con un proveedor que expone
 * /actuator/health. Comparte solo el health check; la traducción del pedido
 * queda en cada adapter concreto.
 */
public abstract class AdapterLogisticaHttp implements ProveedorLogisticaAdapter {

    private static final Logger log = LoggerFactory.getLogger(AdapterLogisticaHttp.class);

    protected final RestClient restClient;

    protected AdapterLogisticaHttp(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
    }

    @Override
    public boolean disponible() {
        try {
            Map<?, ?> salud = restClient.get().uri("/actuator/health").retrieve().body(Map.class);
            return salud != null && "UP".equals(salud.get("status"));
        } catch (Exception e) {
            log.warn("[Broker] Proveedor {} no disponible: {}", nombre(), e.getMessage());
            return false;
        }
    }
}
