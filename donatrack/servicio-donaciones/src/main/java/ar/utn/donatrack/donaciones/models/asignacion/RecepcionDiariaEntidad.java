package ar.utn.donatrack.donaciones.models.asignacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuántas donaciones se asignaron a una entidad beneficiaria en un día.
 *
 * DESNORMALIZACIÓN (Entrega 5): es un contador derivado de la tabla donacion
 * (id_entidad_beneficiaria, fecha_asignacion). Existe para el algoritmo de
 * prioridad a sub-atendidos, que necesita saber cuántas donaciones recibió cada
 * entidad en el último trimestre. Antes lo calculaba leyendo TODAS las
 * donaciones con sus bienes e historial una vez por entidad: con 20 entidades
 * y 60 donaciones, un solo ranking costaba más de mil consultas. Con el
 * contador es una consulta agregada sobre una tabla chica.
 *
 * Es diario y no mensual para respetar exactamente la regla del algoritmo
 * ("últimos 3 meses" contados día por día).
 *
 * Lo mantiene DonacionesRepository en la misma transacción que guarda la
 * asignación, y ReconciliacionRecepcionesService lo recalcula cada noche desde
 * la tabla donacion por si alguna vez se desfasa.
 */
@Entity
@Table(name = "entidad_recepcion_diaria", indexes = @Index(name = "idx_recepcion_fecha", columnList = "fecha"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RecepcionDiariaEntidad {

    @EmbeddedId
    private Clave clave;

    @Column(name = "cantidad", nullable = false)
    private long cantidad;

    public RecepcionDiariaEntidad(UUID idEntidad, LocalDate fecha, long cantidad) {
        this(new Clave(idEntidad, fecha), cantidad);
    }

    /** Clave compuesta: una fila por entidad y por día. */
    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class Clave implements Serializable {

        @Column(name = "entidad_id")
        private UUID idEntidad;

        @Column(name = "fecha")
        private LocalDate fecha;
    }
}
