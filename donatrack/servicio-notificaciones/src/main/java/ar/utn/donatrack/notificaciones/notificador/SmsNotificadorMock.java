package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: SMS.
 * Mock: simula el envío por consola. En producción se integraría con
 * Twilio, AWS SNS u otro proveedor de SMS.
 */
@Component
public class SmsNotificadorMock extends NotificadorBase {

    public MedioNotificacion  getMedio() {
        return new Sms();
    }

    protected String getEtiqueta() {
        return "SMS";
    }
}
