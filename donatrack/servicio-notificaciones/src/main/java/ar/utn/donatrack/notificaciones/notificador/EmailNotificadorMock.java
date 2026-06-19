package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: EMAIL.
 * Mock: simula el envío por consola. En una entrega futura se reemplaza
 * por la llamada real a un proveedor SMTP o servicio de email externo.
 */
@Component
public class EmailNotificadorMock extends NotificadorBase {

    public TipoMedioNotificacion getMedio() {
        return TipoMedioNotificacion.EMAIL;
    }

    protected String getEtiqueta() {
        return "EMAIL";
    }
}
