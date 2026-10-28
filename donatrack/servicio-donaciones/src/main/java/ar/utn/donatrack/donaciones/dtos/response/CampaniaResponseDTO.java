package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
public class CampaniaResponseDTO {
    private final UUID idCampania;
    private final UUID idEntidad;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final String descripcionGeneral;
    private final List<NecesidadResponseDTO> necesidades;
}