package ar.utn.donatrack.donaciones.model.donacion;

import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CargaDonacion {
    private int idCargaDonacion;
    private String descripcionGeneral;
    private PersonaDonante donante;
    private LocalDateTime fechaIngreso;
    private List<Bien> bienes;
}
