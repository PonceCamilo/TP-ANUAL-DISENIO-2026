package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CargaDonacion {
    private int idCargaDonacion;
    private String descripcionGeneral;
    private LocalDateTime fechaIngreso;
    private List<Bien> bienes;
}
