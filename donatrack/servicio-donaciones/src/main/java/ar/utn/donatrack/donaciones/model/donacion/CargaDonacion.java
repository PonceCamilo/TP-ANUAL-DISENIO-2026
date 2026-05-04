package ar.utn.donatrack.donaciones.model.donacion;

import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;

import java.time.LocalDateTime;
import java.util.List;

public class CargaDonacion {
    // TODO: id, descripcionGeneral, fechaIngreso, donante, bienes
    private PersonaDonante donante;
    private LocalDateTime fechaIngreso;
    private List<Bien> bienes;
}
