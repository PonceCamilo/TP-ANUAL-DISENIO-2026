package ar.utn.donatrack.notificaciones.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa una notificación individual enviada a un destinatario
 * por un medio determinado (email, sms o whatsapp).
 */
@Builder
@Getter
@Setter
public class Notificacion {
    private UUID id;
    private String destinatario;
    private String mensaje;
    private TipoMedioNotificacion medio;
    private TipoEvento evento;
    private EstadoNotificacion estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
}