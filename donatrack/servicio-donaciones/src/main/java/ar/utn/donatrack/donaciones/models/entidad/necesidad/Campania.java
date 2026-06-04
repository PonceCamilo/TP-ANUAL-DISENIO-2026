package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Campania {
    private int idCargaNecesidad;
    private String descripcionGeneral;
    private List<Necesidad> necesidades;
    private String razonSocialEntidad;
}
