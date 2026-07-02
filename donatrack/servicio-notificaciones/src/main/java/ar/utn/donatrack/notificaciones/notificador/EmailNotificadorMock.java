package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: EMAIL.
 * Mock: simula el envío por consola. En una entrega futura se reemplaza
 * por la llamada real a un proveedor SMTP o servicio de email externo.
 */
@Component
public class EmailNotificadorMock extends NotificadorBase {

    public MedioNotificacion  getMedio() {
        return new Email();
    }

    protected String getEtiqueta() {
        return "EMAIL";
    }
}
