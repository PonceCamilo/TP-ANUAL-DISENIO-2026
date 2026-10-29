package ar.utn.donatrack.notificaciones.model;

import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.medios.MediosNotificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una notificación individual enviada a un destinatario por un medio
 * determinado (email, sms o whatsapp).
 *
 * MAPEO (Entrega 4): el medio es un objeto de la jerarquía MedioNotificacion,
 * no un enum, y no se persiste. Se guarda su nombre en la columna `medio` y el
 * objeto se rehidrata en @PostLoad con MediosNotificacion. Es el mismo criterio
 * que aplica servicio-donaciones al patrón State.
 *
 * El estado sí es un enum, así que se mapea directo con @Enumerated(STRING):
 * se guarda el nombre y no la posición, para que sobreviva a reordenamientos.
 */
@Entity
@Table(name = "notificacion", indexes = {
        // Optimizan las consultas de lectura (Entrega 5): filtrar por estado
        // (ej. reintentar las FALLIDA), por destinatario (historial de una
        // persona) y ordenar por fecha sin recorrer toda la tabla.
        @Index(name = "idx_notificacion_estado", columnList = "estado"),
        @Index(name = "idx_notificacion_destinatario", columnList = "destinatario"),
        @Index(name = "idx_notificacion_fecha_creacion", columnList = "fecha_creacion")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "destinatario", nullable = false)
    private String destinatario;

    @Column(name = "mensaje", length = 2000)
    private String mensaje;

    /** Nombre del medio: EMAIL, SMS o WHATSAPP. Es lo único que llega a la base. */
    @Column(name = "medio", nullable = false)
    private String medioNombre;

    /** El objeto de medio: se rehidrata desde medioNombre, nunca se persiste. */
    @Transient
    private MedioNotificacion medio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNotificacion estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    /**
     * Qué disparó la notificación (ej. MISION_CUMPLIDA). Opcional: donaciones
     * no lo manda. Se guarda como texto, sin tabla catálogo (desnormalizado a
     * propósito: se lee siempre junto con la notificación y evitamos un join).
     */
    @Column(name = "evento", length = 100)
    private String evento;

    /** Por qué falló el envío (mensaje de la excepción). Solo si estado = FALLIDA. */
    @Column(name = "motivo_fallo", length = 500)
    private String motivoFallo;

    /** Reconstruye el objeto de medio después de cargar la fila. */
    @PostLoad
    private void rehidratarMedio() {
        this.medio = MediosNotificacion.desdeONull(this.medioNombre);
    }

    /**
     * Mantiene sincronizados el objeto de medio y el nombre que se persiste.
     * Sin esto, un setMedio() directo dejaría la columna con el valor viejo.
     */
    public void setMedio(MedioNotificacion medio) {
        this.medio = medio;
        this.medioNombre = medio == null ? null : medio.getNombre();
    }

    /** Marca la notificación como enviada exitosamente y registra el momento del envío. */
    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
    }

    /** Marca la notificación como fallida (no se pudo entregar por el medio elegido). */
    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
    }

    /** Igual que marcarFallida() pero deja registrado el motivo para poder auditarlo. */
    public void marcarFallida(String motivo) {
        marcarFallida();
        this.motivoFallo = motivo == null ? null
                : motivo.substring(0, Math.min(motivo.length(), 500));
    }

    /**
     * Builder con un setter propio para `medio`.
     *
     * Lombok detecta que el método está escrito a mano y no genera el suyo. De
     * esa forma `.medio(new Email())` sigue funcionando igual en el service y en
     * los tests, pero ademas completa `medioNombre`, que es la columna que de
     * verdad se persiste y es NOT NULL. Sin esto, todo insert fallaría.
     */
    public static class NotificacionBuilder {
        public NotificacionBuilder medio(MedioNotificacion medio) {
            this.medio = medio;
            this.medioNombre = medio == null ? null : medio.getNombre();
            return this;
        }
    }
}
