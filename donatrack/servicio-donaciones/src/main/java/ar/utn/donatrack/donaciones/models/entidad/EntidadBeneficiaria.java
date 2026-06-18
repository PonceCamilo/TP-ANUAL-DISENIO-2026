package ar.utn.donatrack.donaciones.models.entidad;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
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
    private List<Representante> representantes;

    @Builder.Default
    private List<Campania> campanias = new ArrayList<>();

    public void agregarCampania(Campania campania) {
        campanias.add(campania);
    }

    public void agregarNecesidadACampania(Campania campania, Necesidad necesidad) {
        this.getCampanias().stream().filter(c -> c.getIdCampania()
                .equals(campania.getIdCampania()))
            .findFirst()
            .ifPresent(c -> c.getNecesidades().add(necesidad));
    }
}