package ar.utn.donatrack.donaciones.models.asignacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resultado del matchmaking de UNA donación, calculado por el proceso nocturno.
 *
 * Guarda las tres listas que devuelven los algoritmos (el ranking de cada uno y
 * la intersección) junto con el momento del cálculo, para que la persona
 * administradora sepa qué tan vigente es la recomendación que está viendo.
 *
 * DESNORMALIZACIÓN (Entrega 5): es una tabla de lectura precalculada. Todo su
 * contenido se puede derivar de las donaciones, las entidades y sus necesidades,
 * pero recalcularlo en cada consulta cuesta los dos algoritmos completos. Se
 * calcula una vez por noche y la bandeja de la persona administradora lo lee
 * tal cual. Hasta la Entrega 4 vivía en un ConcurrentHashMap: se perdía al
 * reiniciar el servicio y cada instancia tenía su propia bandeja.
 *
 * MAPEO: una tabla por lista (ranking_coincidencia, ranking_semantico,
 * ranking_subatendidos), con la posición en el ranking como columna de orden.
 * SUBSELECT hace que leer toda la bandeja cueste cuatro consultas y no cuatro
 * por donación.
 */
@Entity
@Table(name = "ranking_precalculado")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RankingPrecalculado {

    @Id
    @Column(name = "id_donacion")
    private UUID idDonacion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ranking_coincidencia", joinColumns = @JoinColumn(name = "id_donacion"))
    @OrderColumn(name = "posicion")
    @Fetch(FetchMode.SUBSELECT)
    private List<ResultadoAsignacion> coincidencias;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ranking_semantico", joinColumns = @JoinColumn(name = "id_donacion"))
    @OrderColumn(name = "posicion")
    @Fetch(FetchMode.SUBSELECT)
    private List<ResultadoAsignacion> rankingSemantico;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ranking_subatendidos", joinColumns = @JoinColumn(name = "id_donacion"))
    @OrderColumn(name = "posicion")
    @Fetch(FetchMode.SUBSELECT)
    private List<ResultadoAsignacion> rankingSubAtendidos;

    @Column(name = "fecha_calculo")
    private LocalDateTime fechaCalculo;

    /** Las coincidencias son las candidatas de mayor confianza: las recomiendan ambos algoritmos. */
    public boolean huboCoincidencias() {
        return coincidencias != null && !coincidencias.isEmpty();
    }

    /** True si ningún algoritmo encontró una sola entidad candidata. */
    public boolean sinCandidatas() {
        return (rankingSemantico == null || rankingSemantico.isEmpty())
                && (rankingSubAtendidos == null || rankingSubAtendidos.isEmpty());
    }
}
