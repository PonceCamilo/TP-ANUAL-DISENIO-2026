package ar.utn.donatrack.donaciones.models.entidad;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
public class EntidadBeneficiaria {
    
    private int id;
    private String razonSocial;
    private Direccion direccion;
    private String telefono;
    private List<String> correosRepresentantes;

    @Builder.Default
    private List<Necesidad> necesidades = new ArrayList<>();

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }
}