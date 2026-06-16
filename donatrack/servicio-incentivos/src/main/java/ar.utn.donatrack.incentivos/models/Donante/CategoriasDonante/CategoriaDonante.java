package ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante;

import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public abstract class CategoriaDonante{
    // private UUID id;
    private String nombre;
    private List<Mision> misiones;         // son secuenciales. Dependen de la categoria, cada categoria tiene su lista de misiones

    public abstract CategoriaDonante siguienteCategoria();

    // pasar esto a la capa de service 
    public Mision primeraMision(){
        return misiones.get(0);
    };

    public Mision siguienteMision(Mision misionActual){
        int index = misiones.indexOf(misionActual);
        if (index == -1 || index == misiones.size() - 1) {
            // aca deberia subir de categoria y retornar la primera mision de la nueva categoria
        }
        return misiones.get(index + 1);
    }
    
}