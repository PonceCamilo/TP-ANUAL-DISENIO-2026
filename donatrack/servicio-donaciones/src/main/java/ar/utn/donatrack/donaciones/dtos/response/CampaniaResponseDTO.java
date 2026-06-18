package ar.utn.donatrack.donaciones.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaniaResponseDTO {
    private UUID idCampania;
    private UUID idEntidad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String descripcionGeneral;
    private List<NecesidadResponseDTO> necesidades;
}