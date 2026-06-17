package ar.utn.donatrack.incentivos.models.categoriasdonante;

import ar.utn.donatrack.incentivos.models.misiones.Mision;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public abstract class CategoriaDonante{
    private String nombre;
    private List<Mision> misiones;         // son secuenciales. Dependen de la categoria, cada categoria tiene su lista de misiones

    public abstract CategoriaDonante siguienteCategoria();

    // pasar esto a la capa de service 
    public Mision primeraMision(){
        return misiones.get(0);
    }

    public Mision siguienteMision(Mision misionActual){
        int index = misiones.indexOf(misionActual);
        if (index == -1 || index == misiones.size() - 1) {
            return null;
        }
        return misiones.get(index + 1);
    }
    
}