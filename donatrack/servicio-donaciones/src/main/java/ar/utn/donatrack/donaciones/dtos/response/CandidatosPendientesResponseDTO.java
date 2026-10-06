package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Una donación en depósito con las entidades que el matchmaking nocturno le
 * recomendó, a la espera de que una persona administradora confirme el destino.
 *
 * Se diferencia de CandidatosAsignacionResponseDTO (la consulta a demanda) en
 * que incluye cuándo se calculó: el ranking puede haber quedado viejo si las
 * entidades cargaron necesidades nuevas después de la corrida.
 */
@Getter
@Builder
public class CandidatosPendientesResponseDTO {

    private UUID idDonacion;
    private LocalDateTime fechaCalculo;

    /** True si la entidad aparece recomendada por los dos algoritmos a la vez. */
    private boolean huboCoincidencias;

    private List<EntidadBeneficiariaResponseDTO> coincidencias;
    private List<EntidadBeneficiariaResponseDTO> porCompatibilidad;
    private List<EntidadBeneficiariaResponseDTO> porSubatendidos;
}
