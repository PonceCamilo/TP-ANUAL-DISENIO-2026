import ar.utn.donatrack.incentivos.models.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class CategoriaDonante{
    private String nombre;
    private List<Mision> misiones;         // son secuenciales. Dependen de la categoria, cada categoria tiene su lista de misiones

    public CategoriaDonante siguienteCategoria();

    // pasar esto a la capa de service 
    public Mision primeraMision(){
        return misiones.get(0);
    };

    public Mision siguienteMision(Mision misionActual){
        int index = misiones.indexOf(misionActual);
        if (index == -1 || index == misiones.size() - 1) {
            throw new IllegalArgumentException("Misión no encontrada o es la última de la categoría"); // TODO codificar esta excepcion
        }
        return misiones.get(index + 1);
    }
    
}