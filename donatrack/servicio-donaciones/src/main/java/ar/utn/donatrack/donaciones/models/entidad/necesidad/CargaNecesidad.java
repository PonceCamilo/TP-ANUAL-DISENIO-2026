package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CargaNecesidad {
    private int idCargaNecesidad;
    // private String descripcionGeneral;
    private LocalDateTime fechaIngreso;
    private List<Necesidad> necesidades;
}
