package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agrupador de múltiples Necesidades bajo una misma campaña de la entidad.
 * Permite registrar en un solo paso varias necesidades (ej: tras una inundación).
 *
 * idEntidad referencia a la EntidadBeneficiaria propietaria en lugar de
 * almacenar la razón social como String (evita inconsistencias si cambia el nombre).
 */

@Getter
@Setter
public class Campania {

    private UUID idEntidad;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String descripcionGeneral;
    private UUID idCampania;

    private List<Necesidad> necesidades = new ArrayList<>();
}
