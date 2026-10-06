package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;
import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Punto único de salida hacia el Servicio de Notificaciones.
 *
 * Desde la Entrega 4 ya no habla HTTP directamente: arma el mensaje y delega en
 * un NotificacionTransport, que puede ser sincrónico (REST) o asincrónico
 * (cola de RabbitMQ) según la property `notificaciones.transporte`.
 *
 * La firma de enviarNotificacion() NO cambió a propósito: DonacionService,
 * LogisticaEventosService e InactividadDonantesService siguen usándola igual y
 * no saben —ni les importa— por dónde viaja el aviso.
 */
@Component
public class NotificacionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);

    private final NotificacionTransport transporte;

    public NotificacionClient(NotificacionTransport transporte) {
        this.transporte = transporte;
    }

    /** Deja constancia en el arranque de qué transporte quedó activo. */
    @PostConstruct
    public void registrarTransporteActivo() {
        log.info("[NotificacionClient] Transporte de notificaciones: {}", transporte.nombre());
    }

    public void enviarNotificacion(String destinatario, String mensaje, String medio) {
        transporte.enviar(new SolicitudNotificacionMensaje(destinatario, mensaje, medio));
    }
}
