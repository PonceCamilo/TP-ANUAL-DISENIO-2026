package ar.utn.donatrack.brokerlogistica.clientes;

import ar.utn.donatrack.brokerlogistica.exceptions.DonacionesNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reenvía los eventos de logística a los callbacks que ya expone
 * servicio-donaciones (LogisticaEventosController). Es el rol que antes
 * cumplía el workflow de n8n.
 */
@Component
public class DonacionesClient {

    public static final String INICIO_RUTA = "/logistica/eventos/inicio-ruta";
    public static final String ENTREGA_EXITOSA = "/logistica/eventos/entrega-exitosa";
    public static final String ENTREGA_FALLIDA = "/logistica/eventos/entrega-fallida";

    private static final Logger log = LoggerFactory.getLogger(DonacionesClient.class);

    private final RestClient restClient;

    public DonacionesClient(RestClient.Builder builder, @Value("${broker.donaciones.url}") String baseUrl) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
    }

    public void reenviar(String endpoint, Object evento) {
        try {
            restClient.post().uri(endpoint).body(evento).retrieve().toBodilessEntity();
            log.info("[Broker] Evento reenviado a Donaciones {}", endpoint);
        } catch (Exception e) {
            log.error("[Broker] No se pudo reenviar el evento a Donaciones {}: {}", endpoint, e.getMessage());
            throw new DonacionesNoDisponibleException(endpoint, e);
        }
    }
}
