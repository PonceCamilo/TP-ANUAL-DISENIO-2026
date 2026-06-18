package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Envuelve la carga de una donación: una descripción general y la lista de bienes.
 * El sistema segmenta automáticamente estos bienes en donaciones independientes
 * agrupadas por subcategoría.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionRequestDTO {

  @NotBlank
  private String descripcion;

  @NotEmpty
  @Valid
  private List<BienRequestDTO> bienes;
}
