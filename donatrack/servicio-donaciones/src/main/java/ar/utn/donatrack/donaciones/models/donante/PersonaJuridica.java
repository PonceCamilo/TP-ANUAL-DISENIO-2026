package ar.utn.donatrack.donaciones.models.donante;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Getter
@Setter
public class PersonaJuridica extends PersonaDonante {

    private String razonSocial;
    private TipoPersonaJuridica tipo;
    private String rubro;

    @Builder.Default
    private List<Representante> representantes = new ArrayList<>();

    public void agregarRepresentante(Representante representante) {
        representantes.removeIf(rep -> rep.getEmail() != null
                && rep.getEmail().equalsIgnoreCase(representante.getEmail()));
        representantes.add(representante);
    }
}
