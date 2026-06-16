package ar.utn.donatrack.incentivos.models.Donante.Misiones;

import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Completitud extends Mision{
    // consiste en realizar donaciones de x categorias distintas. El premio se otorga al completar donaciones de x categorias distintas. Para esto, se puede llevar un registro de las categorias de las donaciones realizadas por el donante, y verificar si se han completado las categorias requeridas para completar la mision.
    private int categoriasRequeridas; // cantidad de categorias distintas requeridas para completar la mision

    @Override
    public boolean completada(DonanteInfoDTO donanteInfo){
        return progresoActual(donanteInfo) >= categoriasRequeridas;
    }

    @Override
    public int progresoActual(DonanteInfoDTO donanteInfo){
        return donanteInfo.getCantidadCategoriasDonacionesRealizadas();  // TODO ver si esta programado y sino programarlo
    }
}