package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.interfaces.integracion.EntregaEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Observer concreto: traduce un EntregaEvento en un webhook a n8n
 * (mismo mecanismo que N8nWebhookClient en servicio-incentivos).
 *
 * Esto es lo que permite cumplir la restricción "logística no debe invocar
 * directamente a servicio-notificaciones": logística solo le avisa a n8n
 * que ocurrió un hecho; es el flujo de n8n el que arma la notificación y
 * llama a POST /notificaciones.
 */
@Component
public class N8nLogisticaWebhookListener implements EntregaEventListener {

    private static final Logger log = LoggerFactory.getLogger(N8nLogisticaWebhookListener.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public N8nLogisticaWebhookListener(
            @Value("${integraciones.n8n.webhook.url}") String webhookUrl,
            ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void onEvento(EntregaEvento evento) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tipo", evento.getTipo().name());
            payload.put("entregaId", evento.getEntregaId());
            payload.put("idDonacion", evento.getIdDonacion());
            payload.put("idEntidadBeneficiaria", evento.getIdEntidadBeneficiaria());
            payload.put("idDonante", evento.getIdDonante());
            payload.put("camionId", evento.getCamionId());
            payload.put("rutaId", evento.getRutaId());
            payload.put("fotosComprobante", evento.getFotosComprobante());
            payload.put("motivo", evento.getMotivo());
            // Bandera para que el flujo de n8n elija si alerta a un administrador.
            payload.put("requiereAvisoAdmin", evento.getTipo() == TipoEventoLogistica.ENTREGA_NO_RECIBIDA);

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Fire and forget de verdad: envío asíncrono para no bloquear el hilo
            // que atiende la petición. Marcar la entrega no debe quedar esperando
            // a que n8n responda (ni colgarse si n8n está caído o lento).
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((respuesta, error) -> {
                        if (error != null) {
                            log.error("[N8nLogisticaWebhookListener] No se pudo disparar el evento {} para la entrega {}: {}",
                                    evento.getTipo(), evento.getEntregaId(), error.getMessage());
                        } else {
                            log.info("[N8nLogisticaWebhookListener] Evento {} disparado para entrega {}",
                                    evento.getTipo(), evento.getEntregaId());
                        }
                    });
        } catch (Exception e) {
            log.error("[N8nLogisticaWebhookListener] No se pudo preparar el evento {} para la entrega {}: {}",
                    evento.getTipo(), evento.getEntregaId(), e.getMessage());
        }
    }
}
