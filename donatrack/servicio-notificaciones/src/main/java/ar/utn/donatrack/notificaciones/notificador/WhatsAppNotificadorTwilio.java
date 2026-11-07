package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.WhatsApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * PATRÓN STRATEGY — implementación concreta: WHATSAPP.
 * Integración real vía el WhatsApp Sandbox de Twilio: usa el mismo
 * endpoint de mensajes que SMS, con el prefijo "whatsapp:" en los
 * números de origen y destino.
 *
 * Nota para la defensa: el Sandbox es gratuito, pero cada número
 * destino tiene que "unirse" una vez mandándole por WhatsApp un código
 * (ej: "join palabra-clave") al número del Sandbox de Twilio. Es la
 * forma que da Twilio de usar WhatsApp real sin pasar por la
 * aprobación de negocio completa de Meta, que sí sería pertinente en
 * un entorno de producción real.
 */
@Component
public class WhatsAppNotificadorTwilio extends NotificadorBase {

    private final RestClient restClient = RestClient.create();

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from-number}")
    private String fromNumber;

    public MedioNotificacion getMedio() {
        return new WhatsApp();
    }

    @Override
    protected void realizarEnvio(Notificacion notificacion) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", "whatsapp:" + notificacion.getDestinatario());
        body.add("From", "whatsapp:" + fromNumber);
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
        return "WHATSAPP";
    }
}
