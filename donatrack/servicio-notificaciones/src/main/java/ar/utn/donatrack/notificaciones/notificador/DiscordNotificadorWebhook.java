package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Discord;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * PATRÓN STRATEGY — implementación concreta: DISCORD.
 * Integración real: publica el mensaje en un canal de Discord vía
 * webhook entrante (no requiere OAuth ni token de bot, solo una URL
 * secreta configurada del lado del servidor de Discord).
 *
 * El campo "destinatario" de la notificación se antepone al mensaje
 * a modo de mención textual, ya que un webhook de Discord publica
 * siempre en el canal configurado, no le pega a una persona puntual
 * como sí lo hace un email.
 */
@Component
public class DiscordNotificadorWebhook extends NotificadorBase {

    private final RestClient restClient = RestClient.create();

    @Value("${notificaciones.discord.webhook-url}")
    private String webhookUrl;

    public MedioNotificacion getMedio() {
        return new Discord();
    }

    @Override
    protected void realizarEnvio(Notificacion notificacion) {
        String contenido = "**Para: " + notificacion.getDestinatario() + "**\n" + notificacion.getMensaje();

        restClient.post()
                .uri(webhookUrl)
                .body(Map.of("content", contenido))
                .retrieve()
                .toBodilessEntity();
    }

    protected String getEtiqueta() {
        return "DISCORD";
    }
}
