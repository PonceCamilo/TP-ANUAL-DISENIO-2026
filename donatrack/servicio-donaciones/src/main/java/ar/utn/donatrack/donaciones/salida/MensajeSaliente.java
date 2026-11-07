package ar.utn.donatrack.donaciones.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un aviso que el servicio tiene que mandar afuera (servicio de notificaciones,
 * servicio de incentivos), guardado en la MISMA transacción que el cambio que lo
 * origina: es el patrón outbox transaccional.
 *
 * Antes los avisos se mandaban "después" del cambio pero sin garantía: si el
 * servicio externo estaba caído, o el proceso se cortaba justo después del
 * commit, el aviso se perdía. Ahora, si el cambio se confirma, el mensaje queda
 * guardado; RelayDeMensajes lo envía y, si falla, lo reintenta con una espera
 * creciente. La entrega es "al menos una vez": un corte entre el envío y la marca
 * puede repetir un aviso, nunca perderlo.
 */
@Entity
@Table(name = "mensaje_saliente",
        indexes = @Index(name = "idx_mensaje_saliente_listos", columnList = "estado, proximo_intento"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MensajeSaliente implements Persistable<UUID> {

    /** Primera espera antes de reintentar; se duplica en cada falla, hasta una hora. */
    private static final long ESPERA_INICIAL_SEGUNDOS = 30;
    private static final long ESPERA_MAXIMA_SEGUNDOS = 3600;
    private static final int LARGO_ERROR = 1000;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    /** El cuerpo del mensaje en JSON, tal como lo recibe el destino. */
    @Column(name = "payload", nullable = false, length = 10000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoMensaje estado;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "creado", nullable = false)
    private LocalDateTime creado;

    @Column(name = "proximo_intento", nullable = false)
    private LocalDateTime proximoIntento;

    @Column(name = "enviado")
    private LocalDateTime enviado;

    @Column(name = "ultimo_error", length = LARGO_ERROR)
    private String ultimoError;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nuevo = true;

    public static MensajeSaliente nuevo(String destino, String payload, LocalDateTime ahora) {
        MensajeSaliente mensaje = new MensajeSaliente();
        mensaje.id = UUID.randomUUID();
        mensaje.destino = destino;
        mensaje.payload = payload;
        mensaje.estado = EstadoMensaje.PENDIENTE;
        mensaje.creado = ahora;
        mensaje.proximoIntento = ahora;
        return mensaje;
    }

    public void marcarEnviado(LocalDateTime ahora) {
        this.estado = EstadoMensaje.ENVIADO;
        this.enviado = ahora;
        this.intentos++;
        this.ultimoError = null;
    }

    /**
     * Cuenta el intento fallido y programa el próximo con espera exponencial
     * (30 s, 1 min, 2 min… hasta una hora). Al llegar al máximo de intentos queda
     * FALLIDO: deja de reintentarse y queda para revisar en la tabla.
     */
    public void registrarFalla(String error, LocalDateTime ahora, int maxIntentos) {
        this.intentos++;
        this.ultimoError = error == null || error.length() <= LARGO_ERROR ? error : error.substring(0, LARGO_ERROR);
        if (intentos >= maxIntentos) {
            this.estado = EstadoMensaje.FALLIDO;
            return;
        }
        long espera = Math.min(ESPERA_INICIAL_SEGUNDOS << Math.min(intentos - 1, 20), ESPERA_MAXIMA_SEGUNDOS);
        this.proximoIntento = ahora.plusSeconds(espera);
    }

    @Override
    public boolean isNew() {
        return nuevo;
    }

    @PostLoad
    @PostPersist
    void marcarPersistido() {
        this.nuevo = false;
    }
}
