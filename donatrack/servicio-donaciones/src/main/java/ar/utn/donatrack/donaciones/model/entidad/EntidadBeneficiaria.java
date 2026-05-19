package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;

@Getter
@Setter
public class EntidadBeneficiaria {
    
    private int id;
    private String razonSocial;
    private Direccion direccion;
    private String telefono;
    private List<String> correosRepresentantes = new ArrayList<>();
    private List<Necesidad> necesidades = new ArrayList<>();

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

}