package ar.utn.donatrack.donaciones.models.asignacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un puesto del ranking de un algoritmo: qué entidad quedó en qué posición y con
 * qué puntaje.
 *
 * MAPEO (Entrega 5, revisado): todos los rankings de una donación viven en una
 * sola tabla, ranking_puesto, con el nombre del algoritmo como dato. Antes había
 * una tabla por lista (ranking_semantico, ranking_subatendidos y
 * ranking_coincidencia), iguales salvo el nombre. La posición es una columna
 * explícita y no un @OrderColumn porque cada algoritmo numera desde cero.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PuestoRanking {

    @Column(name = "algoritmo", nullable = false)
    private String algoritmo;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Embedded
    private ResultadoAsignacion resultado;
}
