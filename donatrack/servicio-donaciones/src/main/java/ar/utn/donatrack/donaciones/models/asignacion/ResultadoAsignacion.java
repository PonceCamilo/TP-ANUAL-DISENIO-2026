package ar.utn.donatrack.donaciones.models.asignacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Representa el resultado de evaluar una entidad beneficiaria
 * frente a una donación segun un algoritmo de asignación.
 * El puntaje determina el orden dentro del ranking (mayor = mejor candidata).
 *
 * MAPEO (Entrega 5): es un objeto de valor (@Embeddable) que vive dentro de las
 * listas de RankingPrecalculado; no tiene identidad propia.
 */
@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResultadoAsignacion {

    @Column(name = "id_entidad")
    private UUID idEntidad;

    @Column(name = "puntaje")
    private double puntaje;
}
