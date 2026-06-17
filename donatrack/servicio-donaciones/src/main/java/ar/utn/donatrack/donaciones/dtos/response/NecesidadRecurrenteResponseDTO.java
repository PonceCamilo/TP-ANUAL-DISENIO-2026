package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.Periodicidad;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NecesidadRecurrenteResponseDTO extends NecesidadResponseDTO {

    //private Periodicidad periodo;
    private LocalDate fechaInicioPeriodo;