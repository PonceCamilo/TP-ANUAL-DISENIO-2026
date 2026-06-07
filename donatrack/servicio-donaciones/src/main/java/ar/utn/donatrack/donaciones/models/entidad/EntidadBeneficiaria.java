package ar.utn.donatrack.donaciones.models.entidad;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;

@Builder
@Getter
@Setter
public class EntidadBeneficiaria {
    
    private UUID id;
    private String razonSocial;
    private Direccion direccion;
    private List<MedioDeContacto> contactos;
    private List<String> correosRepresentantes;

    @Builder.Default
    private List<Necesidad> necesidades = new ArrayList<>();

    public void registrarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }
}