import ar.utn.donatrack.incentivos.models.CategoriasDonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.Insignias.InsginiaObtenidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Donante{
    // private String nombreUsuario;
    private CategoriaDonante categoria;
    private List<Insignia> insignias;
    private EstadoMision misionActual;
    private List<InsginiaObtenidad> insignias;
}