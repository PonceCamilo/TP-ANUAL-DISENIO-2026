package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: SMS.
 * Mock: simula el envío por consola. En producción se integraría con
 * Twilio, AWS SNS u otro proveedor de SMS.
 */
@Component
public class SmsNotificadorMock extends NotificadorBase {

    public TipoMedioNotificacion getMedio() {
        return TipoMedioNotificacion.SMS;
    }

    protected String getEtiqueta() {
        return "SMS";
    }
}
