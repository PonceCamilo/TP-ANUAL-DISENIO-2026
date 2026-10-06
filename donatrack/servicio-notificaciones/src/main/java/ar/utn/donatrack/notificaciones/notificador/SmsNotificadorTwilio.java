package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * PATRÓN STRATEGY — implementación concreta: SMS.
 * Integración real vía la API de Twilio (sin agregar el SDK de Twilio
 * como dependencia: es una API REST simple, un POST con Basic Auth
 * alcanza — mismo criterio minimalista que DiscordNotificadorWebhook).
 *
 * Nota para la defensa: con una cuenta trial de Twilio (gratuita), los
 * SMS solo pueden mandarse a números verificados manualmente en el
 * panel de Twilio. Es una limitación del plan gratuito, no del diseño.
 */
@Component
public class SmsNotificadorTwilio extends NotificadorBase {

    private final RestClient restClient = RestClient.create();

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.sms-from-number}")
    private String fromNumber;

    public MedioNotificacion getMedio() {
        return new Sms();
    }

    @Override
    protected void realizarEnvio(Notificacion notificacion) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", notificacion.getDestinatario());
        body.add("From", fromNumber);
        body.add("Body", notificacion.getMensaje());

        restClient.post()
                .uri(url)
                .headers(h -> h.setBasicAuth(accountSid, authToken))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    protected String getEtiqueta() {
        return "SMS";
    }
}
