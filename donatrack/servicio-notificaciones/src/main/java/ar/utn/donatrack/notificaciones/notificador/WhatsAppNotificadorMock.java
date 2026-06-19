package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: WHATSAPP.
 * Mock: simula el envío por consola. En producción se integraría con
 * la API de Meta (WhatsApp Business API).
 */
@Component
public class WhatsAppNotificadorMock extends NotificadorBase {

    public TipoMedioNotificacion getMedio() {
        return TipoMedioNotificacion.WHATSAPP;
    }

    protected String getEtiqueta() {
        return "WHATSAPP";
    }
}
