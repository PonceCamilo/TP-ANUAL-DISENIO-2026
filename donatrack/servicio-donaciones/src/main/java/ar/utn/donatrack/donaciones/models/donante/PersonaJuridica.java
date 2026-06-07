package ar.utn.donatrack.donaciones.models.donante;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@Setter
public class PersonaJuridica extends PersonaDonante {

    private String razonSocial;
    private TipoPersonaJuridica tipo;
    private String rubro;
    private List<Representante> representantes;
}
