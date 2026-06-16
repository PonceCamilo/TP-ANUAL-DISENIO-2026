import ar.utn.donatrack.incentivos.models.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Completitud extends Mision{
    // consiste en realizar donaciones de x categorias distintas. El premio se otorga al completar donaciones de x categorias distintas. Para esto, se puede llevar un registro de las categorias de las donaciones realizadas por el donante, y verificar si se han completado las categorias requeridas para completar la mision.
    int categoriasRequeridas; // cantidad de categorias distintas requeridas para completar la mision

    public boolean completada(Donante donante){
        if(progresoActual(donante) >= categoriasRequeridas){
            return true;
        }
        return false;
    }

    public int progresoActual(Donante donante){
        return donante.cantidadDeCategoriasDonacionesRealizadas();  // TODO ver si esta programado y sino programarlo
    }
}