package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EntidadBeneficiaria {
    
    private int id;
    private String razonSocial;
    private String direccion; // ver si es string o si hacemos una clase direccion
    private String telefono;
    private List<String> correosRepresentantes = new ArrayList<>();;
    private List<Necesidad> necesidades = new ArrayList<>();

    public void registrarNecesidad(Necesidad necesidad) {
        this.necesidades.add(necesidad);
    }
}
