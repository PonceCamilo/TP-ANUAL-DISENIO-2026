package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import ar.utn.donatrack.donaciones.salida.Destinos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Punto único de salida hacia el Servicio de Notificaciones.
 *
 * Desde la Entrega 5 no envía en el momento: deja el aviso en la bandeja de
 * salida, en la transacción de quien llama, y RelayDeMensajes lo manda por el
 * transporte configurado (REST o la cola de RabbitMQ) y lo reintenta si falla.
 *
 * La firma de enviarNotificacion() NO cambió a propósito: Notificador y quienes
 * lo usan no saben —ni les importa— por dónde ni cuándo viaja el aviso.
 */
@Component
@RequiredArgsConstructor
public class NotificacionClient {

    private final BandejaDeSalida salida;

    public void enviarNotificacion(String destinatario, String mensaje, String medio) {
        salida.encolar(Destinos.NOTIFICACION, new SolicitudNotificacionMensaje(destinatario, mensaje, medio));
    }
}
