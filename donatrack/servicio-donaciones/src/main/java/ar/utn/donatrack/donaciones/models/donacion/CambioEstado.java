package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.estado.EstadoDonacionBase;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una entrada del historial de estados de una donación.
 *
 * Es la trazabilidad que pide el enunciado: quedan registrados el estado previo,
 * el nuevo, el nombre de la transición, la justificación y el momento exacto.
 *
 * MAPEO (Entrega 4): se guardan los NOMBRES de los estados, no los objetos. Los
 * getters siguen devolviendo el objeto de estado, reconstruido al vuelo con
 * EstadosDonacion, así que ni el mapper ni los tests notaron el cambio.
 */
@Entity
@Table(name = "cambio_estado")
@Getter
@NoArgsConstructor
public class CambioEstado {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "estado_previo")
    private String estadoPrevioNombre;

    @Column(name = "estado", nullable = false)
    private String estadoNombre;

    @Column(name = "nombre_transicion")
    private String nombreTransicion;

    @Column(name = "justificacion", length = 1000)
    private String justificacion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = FechaHoraArgentina.ahora();

    private CambioEstado(EstadoDonacionBase estadoPrevio, EstadoDonacionBase estado,
                         String nombreTransicion, String justificacion) {
        this.estadoPrevioNombre = estadoPrevio == null ? null : estadoPrevio.nombre();
        this.estadoNombre = estado.nombre();
        this.nombreTransicion = nombreTransicion;
        this.justificacion = justificacion;
    }

    public static CambioEstado de(EstadoDonacionBase estadoPrevio, EstadoDonacionBase estado,
                                  String nombreTransicion, String justificacion) {
        return new CambioEstado(estadoPrevio, estado, nombreTransicion, justificacion);
    }

    /** Reconstruye el objeto de estado previo a partir del nombre guardado. */
    public EstadoDonacionBase getEstadoPrevio() {
        return EstadosDonacion.desdeONull(estadoPrevioNombre);
    }

    /** Reconstruye el objeto de estado resultante a partir del nombre guardado. */
    public EstadoDonacionBase getEstado() {
        return EstadosDonacion.desde(estadoNombre);
    }
}
