package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de ejecutar los algoritmos de asignación sobre una donación.
 * Expone el ranking de cada algoritmo y las coincidencias entre ambos,
 * para que una persona administradora confirme el destino final.
 */
@Getter
@Builder
public class CandidatosAsignacionResponseDTO {
  private UUID idDonacion;
  private List<EntidadBeneficiariaResponseDTO> porCompatibilidad;
  private List<EntidadBeneficiariaResponseDTO> porSubatendidos;
  private List<EntidadBeneficiariaResponseDTO> coincidencias;
}
