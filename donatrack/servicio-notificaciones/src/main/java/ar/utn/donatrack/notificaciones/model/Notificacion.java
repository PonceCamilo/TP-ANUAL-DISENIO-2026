package ar.utn.donatrack.notificaciones.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una notificación a enviar a un destinatario.
 * Registra el resultado del intento de envío (estado).
 */
public class Notificacion {

    private final UUID id;
    private final String destinatario;   // email, número de tel o número WhatsApp
    private final String mensaje;
    private final MedioNotificacion medio;
    private EstadoNotificacion estado;
    private final LocalDateTime creadaEn;
    private LocalDateTime enviadaEn;

    public Notificacion(String destinatario, String mensaje, MedioNotificacion medio) {
        this.id           = UUID.randomUUID();
        this.destinatario = destinatario;
        this.mensaje      = mensaje;
        this.medio        = medio;
        this.estado       = EstadoNotificacion.PENDIENTE;
        this.creadaEn     = LocalDateTime.now();
    }

    public void marcarEnviada() {
        this.estado    = EstadoNotificacion.ENVIADA;
        this.enviadaEn = LocalDateTime.now();
    }

    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
    }

    public UUID getId()                    { return id; }
    public String getDestinatario()        { return destinatario; }
    public String getMensaje()             { return mensaje; }
    public MedioNotificacion getMedio()    { return medio; }
    public EstadoNotificacion getEstado()  { return estado; }
    public LocalDateTime getCreadaEn()     { return creadaEn; }
    public LocalDateTime getEnviadaEn()    { return enviadaEn; }
}
