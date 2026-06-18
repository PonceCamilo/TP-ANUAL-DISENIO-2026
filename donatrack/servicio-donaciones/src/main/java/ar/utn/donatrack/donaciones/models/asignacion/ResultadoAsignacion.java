package ar.utn.donatrack.donaciones.models.asignacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Representa el resultado de evaluar una entidad beneficiaria
 * frente a una donación segun un algoritmo de asignación.
 * El puntaje determina el orden dentro del ranking (mayor = mejor candidata).
 */
@Getter
@Setter
@AllArgsConstructor
public class ResultadoAsignacion {
    private UUID idEntidad;
    private double puntaje;
}