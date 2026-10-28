package ar.utn.donatrack.donaciones.models.asignacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resultado del matchmaking de UNA donación, calculado por el proceso nocturno.
 *
 * Guarda el ranking de cada algoritmo junto con el momento del cálculo, para que
 * la persona administradora sepa qué tan vigente es la recomendación que está
 * viendo. Las coincidencias no se guardan: se derivan de los rankings.
 *
 * DESNORMALIZACIÓN (Entrega 5): es una tabla de lectura precalculada. Todo su
 * contenido se puede derivar de las donaciones, las entidades y sus necesidades,
 * pero recalcularlo en cada consulta cuesta los algoritmos completos. Se calcula
 * una vez por noche y la bandeja de la persona administradora lo lee tal cual.
 *
 * MAPEO: una sola colección de puestos (ver PuestoRanking). Leer toda la bandeja
 * cuesta dos consultas: los puestos se cargan al usarse, y SUBSELECT los trae
 * todos de una vez repitiendo la consulta de los rankings, sin el tope de lote
 * del batch fetch. Vaciarla cuesta dos DELETE y un algoritmo nuevo no cambia el
 * esquema.
 *
 * Cada corrida reemplaza la bandeja entera, así que un ranking siempre se
 * inserta como nuevo (Persistable): sin eso, guardar con el id de la donación
 * haría una lectura previa por ranking.
 */
@Entity
@Table(name = "ranking_precalculado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingPrecalculado implements Persistable<UUID> {

    @Id
    @Column(name = "id_donacion")
    private UUID idDonacion;

    @ElementCollection
    @CollectionTable(name = "ranking_puesto", joinColumns = @JoinColumn(name = "id_donacion"))
    @OrderBy("algoritmo ASC, posicion ASC")
    @Fetch(FetchMode.SUBSELECT)
    @Getter(AccessLevel.NONE)
    private List<PuestoRanking> puestos = new ArrayList<>();

    @Column(name = "fecha_calculo")
    private LocalDateTime fechaCalculo;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nuevo = true;

    public static RankingPrecalculado de(UUID idDonacion, ResultadoMatchmaking resultado, LocalDateTime fechaCalculo) {
        RankingPrecalculado ranking = new RankingPrecalculado();
        ranking.idDonacion = idDonacion;
        ranking.fechaCalculo = fechaCalculo;
        resultado.rankings().forEach((algoritmo, lista) -> {
            for (int posicion = 0; posicion < lista.size(); posicion++) {
                ranking.puestos.add(new PuestoRanking(algoritmo, posicion, lista.get(posicion)));
            }
        });
        return ranking;
    }

    /** Los rankings por algoritmo, rearmados a partir de los puestos guardados. */
    public ResultadoMatchmaking resultado() {
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        for (PuestoRanking puesto : puestos) {
            rankings.computeIfAbsent(puesto.getAlgoritmo(), algoritmo -> new ArrayList<>()).add(puesto.getResultado());
        }
        return new ResultadoMatchmaking(rankings);
    }

    public boolean huboCoincidencias() {
        return resultado().huboCoincidencias();
    }

    public boolean sinCandidatas() {
        return puestos.isEmpty();
    }

    @Override
    public UUID getId() {
        return idDonacion;
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
