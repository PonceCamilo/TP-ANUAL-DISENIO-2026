package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.WhatsApp;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: WHATSAPP.
 * Mock: simula el envío por consola. En producción se integraría con
 * la API de Meta (WhatsApp Business API).
 */
@Component
public class WhatsAppNotificadorMock extends NotificadorBase {

    public MedioNotificacion  getMedio() {return new WhatsApp();}

    protected String getEtiqueta() {
        return "WHATSAPP";
    }
}
