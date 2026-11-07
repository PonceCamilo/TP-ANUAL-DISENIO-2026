package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de ejecutar los algoritmos de asignación sobre una donación.
 * Expone el ranking de cada algoritmo (por nombre) y las coincidencias entre
 * todos, para que una persona administradora confirme el destino final.
 *
 * Antes tenía un campo por algoritmo (porCompatibilidad, porSubatendidos): con
 * la lista de rankings, sumar un algoritmo no cambia el JSON.
 */
@Getter
@Builder
public class CandidatosAsignacionResponseDTO {
    private final UUID idDonacion;
    private final List<RankingAlgoritmoResponseDTO> rankings;
    private final List<EntidadBeneficiariaResponseDTO> coincidencias;
}
